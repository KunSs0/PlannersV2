import com.gitee.planners.core.condition.LearningCost
import com.gitee.planners.core.condition.LearningCosts
import com.gitee.planners.core.condition.SkillPointLearningCost
import com.gitee.planners.core.database.ProfileSnapshot
import com.gitee.planners.core.database.ProfileStore
import com.gitee.planners.core.database.RecordIdentity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.sqlite.SQLiteDataSource
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

/** 使用真实 SQLite 验证已发布学习结果的后台事务，不把外部资源交给 JDBC。 */
class SkillTreePurchaseTest {
    /** 节点、最终点数和首次技能记录仍在一个数据库事务中提交。 */
    @Test
    fun persistsPublishedLearningAtomically() {
        Fixture().use { fixture ->
            val node = fixture.node()
            val skill = fixture.skill()
            fixture.store.saveLearning(fixture.router(4, 3), RecordIdentity(9), node, skill)
            assertEquals(1, fixture.scalar("SELECT level FROM test_skill_tree_node"))
            assertEquals(4, fixture.scalar("SELECT sp_current FROM test_router"))
            assertEquals(3, fixture.scalar("SELECT sp_used FROM test_router"))
            assertEquals(1, fixture.scalar("SELECT COUNT(*) FROM test_skill"))
            assertTrue(node.identity.persisted() > 0)
            assertTrue(skill.identity.persisted() > 0)
        }
    }

    /** JDBC 写入失败只回滚数据库；主线程此前扣除的外部资源保持成功。 */
    @Test
    fun databaseFailureDoesNotRefundPublishedBusiness() {
        for (table in listOf("skill_tree_node", "skill")) {
            Fixture().use { fixture ->
                fixture.execute("CREATE TRIGGER fail_save BEFORE INSERT ON test_$table BEGIN SELECT RAISE(ABORT, 'save failed'); END")
                val resource = Resource()
                LearningCosts(listOf(resource, SkillPointLearningCost(1))).commit()
                val node = fixture.node()
                val skill = fixture.skill()
                assertThrows(SQLException::class.java) {
                    fixture.store.saveLearning(fixture.router(4, 3), RecordIdentity(9), node, skill)
                }
                assertEquals(2, resource.remaining)
                assertEquals(0, resource.rollbacks)
                assertEquals(5, fixture.scalar("SELECT sp_current FROM test_router"))
                assertEquals(0, fixture.scalar("SELECT COUNT(*) FROM test_skill_tree_node"))
                assertEquals(0, fixture.scalar("SELECT COUNT(*) FROM test_skill"))
                assertThrows(IllegalStateException::class.java) { node.identity.persisted() }
                assertThrows(IllegalStateException::class.java) { skill.identity.persisted() }
            }
        }
    }

    /** JDBC commit 最后失败时撤销 SQL，已经成功的外部扣费仍不恢复。 */
    @Test
    fun commitFailureRollsBackDatabaseWithoutRefundingResources() {
        Fixture().use { fixture ->
            val resource = Resource()
            LearningCosts(listOf(resource)).commit()
            val connection = org.mockito.Mockito.spy(fixture.source.connection)
            org.mockito.Mockito.doThrow(SQLException("commit failed")).`when`(connection).commit()
            val source = org.mockito.Mockito.mock(javax.sql.DataSource::class.java)
            org.mockito.Mockito.`when`(source.connection).thenReturn(connection)
            val store = ProfileStore(source, "test")
            assertThrows(SQLException::class.java) {
                store.saveLearning(fixture.router(4, 3), RecordIdentity(9), fixture.node(), fixture.skill())
            }
            assertEquals(2, resource.remaining)
            assertEquals(0, resource.rollbacks)
            assertEquals(5, fixture.scalar("SELECT sp_current FROM test_router"))
            assertEquals(0, fixture.scalar("SELECT COUNT(*) FROM test_skill_tree_node"))
        }
    }

    /** 连续升级沿用同一身份，后一个快照不依赖主线程重新加载数据库。 */
    @Test
    fun upgradesPendingRuntimeIdentityWithoutDuplicateRecords() {
        Fixture().use { fixture ->
            val node = fixture.node()
            fixture.store.saveLearning(fixture.router(4, 3), RecordIdentity(9), node, null)
            val next = ProfileSnapshot.Node(node.identity, "tree", "node", 2)
            fixture.store.saveLearning(fixture.router(3, 4), RecordIdentity(9), next, null)
            assertEquals(2, fixture.scalar("SELECT level FROM test_skill_tree_node"))
            assertEquals(1, fixture.scalar("SELECT COUNT(*) FROM test_skill_tree_node"))
            assertEquals(3, fixture.scalar("SELECT sp_current FROM test_router"))
        }
    }

    /** 从数据库加载的零级节点升级时使用原主键，不插入重复记录。 */
    @Test
    fun upgradesLoadedZeroLevelRecord() {
        Fixture().use { fixture ->
            fixture.execute("INSERT INTO test_skill_tree_node VALUES (77,9,'tree','node',0)")
            val node = ProfileSnapshot.Node(RecordIdentity(77), "tree", "node", 1)
            fixture.store.saveLearning(fixture.router(4, 3), RecordIdentity(9), node, null)
            assertEquals(77, fixture.scalar("SELECT id FROM test_skill_tree_node"))
            assertEquals(1, fixture.scalar("SELECT level FROM test_skill_tree_node"))
        }
    }

    /** 外部资源提交本身失败仍逆序恢复，数据库不在此回滚边界内。 */
    @Test
    fun resourceFailureRollsBackOnlyUnpublishedBusiness() {
        val first = Resource()
        val second = Resource()
        second.failCommit = true
        second.failRollback = true
        val costs = LearningCosts(listOf(first, second))
        val failure = assertThrows(IllegalStateException::class.java) { costs.commit() }
        assertEquals(3, first.remaining)
        assertEquals(1, first.rollbacks)
        assertEquals("resource rollback failed", failure.suppressed.single().message)
    }

    /** 成本负数和溢出仍在任何扣除之前被拒绝。 */
    @Test
    fun invalidCostsFailBeforeConsumption() {
        assertThrows(IllegalArgumentException::class.java) { SkillPointLearningCost(-1) }
        val resource = Resource()
        val costs = LearningCosts(listOf(resource, SkillPointLearningCost(Int.MAX_VALUE), SkillPointLearningCost(1)))
        assertThrows(ArithmeticException::class.java) { costs.skillPoints() }
        assertEquals(3, resource.remaining)
    }

    /** 完成通知异常发生在独立扣费之后，不触发退款。 */
    @Test
    fun notificationFailureDoesNotRefundCommittedResources() {
        val resource = Resource()
        assertThrows(IllegalStateException::class.java) {
            LearningCosts(listOf(resource)).commit()
            error("notification failed")
        }
        assertEquals(2, resource.remaining)
        assertEquals(0, resource.rollbacks)
    }

    /** 合成库夹具，SQL 由真实 ProfileStore 执行。 */
    private class Fixture : AutoCloseable {
        val source = SQLiteDataSource()
        private val anchor: Connection
        val store: ProfileStore

        init {
            source.url = "jdbc:sqlite:file:purchase-${UUID.randomUUID()}?mode=memory&cache=shared"
            anchor = source.connection
            store = ProfileStore(source, "test")
            execute("CREATE TABLE test_router(id INTEGER PRIMARY KEY, level INTEGER, experience INTEGER, current_route INTEGER, sp_current INTEGER, sp_used INTEGER)")
            execute("CREATE TABLE test_skill_tree_node(id INTEGER PRIMARY KEY AUTOINCREMENT, route INTEGER, tree TEXT, node TEXT, level INTEGER)")
            execute("CREATE TABLE test_skill(id INTEGER PRIMARY KEY AUTOINCREMENT, route INTEGER, node TEXT, level INTEGER, equipped INTEGER, backpack_page TEXT, backpack_slot TEXT)")
            execute("INSERT INTO test_router VALUES (7,1,0,9,5,2)")
        }

        /** 创建最终点数快照，字段值由测试场景指定。 */
        fun router(points: Int, used: Int): ProfileSnapshot.Router {
            return ProfileSnapshot.Router(RecordIdentity(7), 1, "router", 1, 0, RecordIdentity(9), points, used)
        }

        /** 创建尚未插入数据库的运行时节点。 */
        fun node(): ProfileSnapshot.Node {
            return ProfileSnapshot.Node(RecordIdentity(RecordIdentity.next()), "tree", "node", 1)
        }

        /** 创建尚未插入数据库的运行时技能。 */
        fun skill(): ProfileSnapshot.Skill {
            return ProfileSnapshot.Skill(RecordIdentity(RecordIdentity.next()), "synthetic_skill", 0, false, null, null)
        }

        /** 执行固定合成 SQL，不连接生产数据库。 */
        fun execute(sql: String) {
            anchor.createStatement().use { statement -> statement.execute(sql) }
        }

        /** 从独立连接查询后台提交后的单值。 */
        fun scalar(sql: String): Int {
            source.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(sql).use { rows ->
                        assertTrue(rows.next())
                        return rows.getInt(1)
                    }
                }
            }
        }

        /** 关闭保持内存数据库存活的锚点连接。 */
        override fun close() {
            anchor.close()
        }
    }

    /** 可注入提交及恢复失败的外部资源，业务与 SQL 不在这里模拟。 */
    private class Resource : LearningCost {
        var remaining = 3
        var rollbacks = 0
        var failCommit = false
        var failRollback = false
        private var committed = false

        /** 本资源不包含技能点。 */
        override fun skillPoints(): Int {
            return 0
        }

        /** 模拟主线程外部资源提交。 */
        override fun commit() {
            committed = true
            remaining -= 1
            if (failCommit) {
                error("resource commit failed")
            }
        }

        /** 逆序恢复时保留可见异常。 */
        override fun rollback() {
            rollbacks += 1
            if (failRollback) {
                error("resource rollback failed")
            }
            if (committed) {
                remaining += 1
            }
        }
    }
}
