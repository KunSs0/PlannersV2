import com.gitee.planners.core.condition.LearningCost
import com.gitee.planners.core.condition.LearningCosts
import com.gitee.planners.core.condition.SkillPointLearningCost
import com.gitee.planners.core.database.SkillTreePurchaseStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/** 使用真实 SQLite 事务验证通用学习编排，不引入任何本服业务或道具插件。 */
class SkillTreePurchaseTest {
    /** 节点、两项技能点和初次技能记录一起提交，重开查询可读取成功状态。 */
    @Test
    fun commitsPointsNodeAndInitialSkillTogether() {
        database().use { connection ->
            val cost = Resource(3)
            val result = purchase(connection, cost, "synthetic_skill")
            assertEquals(1, result.state.level)
            assertNotNull(result.skill)
            assertEquals(2, cost.remaining)
            assertEquals(1, cost.commits)
            assertEquals(0, cost.rollbacks)
            assertEquals(4, scalar(connection, "SELECT sp_current FROM test_router"))
            assertEquals(3, scalar(connection, "SELECT sp_used FROM test_router"))
            assertEquals(1, scalar(connection, "SELECT level FROM test_skill_tree_node"))
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM test_skill"))
        }
    }

    /** 节点 INSERT 失败回滚先前的技能点 SQL，外部资源完全不扣。 */
    @Test
    fun nodeSaveFailureRollsBackPointsWithoutConsumingResources() {
        database().use { connection ->
            execute(connection, "CREATE TRIGGER fail_node BEFORE INSERT ON test_skill_tree_node BEGIN SELECT RAISE(ABORT, 'node save failed'); END")
            val cost = Resource(3)
            assertThrows(SQLException::class.java) { purchase(connection, cost) }
            assertUnchanged(connection, cost)
            assertEquals(0, cost.commits)
        }
    }

    /** 初次技能记录失败时，已经插入的节点和技能点 SQL 同时回滚。 */
    @Test
    fun skillSaveFailureRollsBackNodeAndPoints() {
        database().use { connection ->
            execute(connection, "CREATE TRIGGER fail_skill BEFORE INSERT ON test_skill BEGIN SELECT RAISE(ABORT, 'skill save failed'); END")
            val cost = Resource(3)
            assertThrows(SQLException::class.java) { purchase(connection, cost, "synthetic_skill") }
            assertUnchanged(connection, cost)
        }
    }

    /** 外部资源部分提交后抛错，SQL 与已经扣除的资源均恢复。 */
    @Test
    fun partialResourceFailureRestoresResourcesAndSql() {
        database().use { connection ->
            val cost = Resource(3)
            cost.failCommit = true
            assertThrows(IllegalStateException::class.java) { purchase(connection, cost) }
            assertUnchanged(connection, cost)
            assertEquals(1, cost.commits)
            assertEquals(1, cost.rollbacks)
        }
    }

    /** 最后 JDBC commit 失败，恢复已扣外部资源并撤销节点及技能点。 */
    @Test
    fun databaseCommitFailureRestoresAllCosts() {
        database().use { connection ->
            val failing = Mockito.spy(connection)
            Mockito.doThrow(SQLException("commit failed")).`when`(failing).commit()
            val cost = Resource(3)
            assertThrows(SQLException::class.java) { purchase(failing, cost) }
            assertUnchanged(connection, cost)
        }
    }

    /** 已提交后的事件或连接清理异常不在退款边界内。 */
    @Test
    fun postCommitFailureDoesNotRefundCommittedPurchase() {
        database().use { connection ->
            val cost = Resource(3)
            assertThrows(IllegalStateException::class.java) {
                purchase(connection, cost)
                error("synthetic notification failure")
            }
            assertEquals(2, cost.remaining)
            assertEquals(0, cost.rollbacks)
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM test_skill_tree_node"))
        }
    }

    /** 同请求重放或陈旧技能点快照均被拒绝，不发生第二次消费。 */
    @Test
    fun staleBalanceOrRepeatedNodeDoesNotChargeAgain() {
        database().use { connection ->
            purchase(connection, Resource(3))
            connection.autoCommit = true
            val repeated = Resource(3)
            assertThrows(IllegalStateException::class.java) { purchase(connection, repeated) }
            assertEquals(3, repeated.remaining)
            assertEquals(0, repeated.commits)
            assertEquals(4, scalar(connection, "SELECT sp_current FROM test_router"))
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM test_skill_tree_node"))
        }
    }

    /** 已存在的零级记录和后续升级均保持主键，只变更等级。 */
    @Test
    fun upgradesExistingStateWithoutCreatingDuplicate() {
        database().use { connection ->
            execute(connection, "INSERT INTO test_skill_tree_node VALUES (77, 9, 'tree', 'node', 0)")
            val first = purchase(connection, Resource(3))
            assertEquals(77L, first.state.index)
            connection.autoCommit = true
            val second = LearningCosts(listOf(SkillPointLearningCost(1))).purchase { amount, commit ->
                SkillTreePurchaseStore("test").purchase(connection, 7, 9, "tree", "node", 1, 4, 3, amount, null, 0, commit)
            }
            assertEquals(77L, second.state.index)
            assertEquals(2, second.state.level)
            assertEquals(3, scalar(connection, "SELECT sp_current FROM test_router"))
            assertEquals(4, scalar(connection, "SELECT sp_used FROM test_router"))
        }
    }

    /** 余额不足或成本合计溢出在任何扣除之前拒绝。 */
    @Test
    fun invalidPointsFailBeforeResourceCommit() {
        assertThrows(IllegalArgumentException::class.java) { SkillPointLearningCost(-1) }
        database().use { connection ->
            val resource = Resource(3)
            val costs = LearningCosts(listOf(resource, SkillPointLearningCost(6)))
            assertThrows(IllegalArgumentException::class.java) {
                costs.purchase { amount, commit ->
                    SkillTreePurchaseStore("test").purchase(connection, 7, 9, "tree", "node", 0, 5, 2, amount, null, 0, commit)
                }
            }
            assertUnchanged(connection, resource)
        }
        val resource = Resource(3)
        val overflow = LearningCosts(listOf(resource, SkillPointLearningCost(Int.MAX_VALUE), SkillPointLearningCost(1)))
        assertThrows(ArithmeticException::class.java) { overflow.purchase<Unit> { _, _ -> fail("成本溢出不得进入数据库") } }
        assertEquals(0, resource.commits)
    }

    /** 多个资源按逆序回滚；某项恢复失败不会跳过其他项或隐藏错误。 */
    @Test
    fun rollbackFailuresRemainVisibleAndOtherResourcesStillRestore() {
        val first = Resource(3)
        val second = Resource(3)
        second.failCommit = true
        second.failRollback = true
        val costs = LearningCosts(listOf(first, second))
        val failure = assertThrows(IllegalStateException::class.java) {
            costs.purchase<Unit> { _, commit -> commit() }
        }
        assertEquals("resource commit failed", failure.message)
        assertEquals("resource rollback failed", failure.suppressed.single().message)
        assertEquals(3, first.remaining)
        assertEquals(1, first.rollbacks)
    }

    /** 运行真实学习事务，资源只模拟宿主边界。 */
    private fun purchase(connection: Connection, cost: Resource, skill: String? = null): SkillTreePurchaseStore.Result {
        return LearningCosts(listOf(cost, SkillPointLearningCost(1))).purchase { amount, commit ->
            SkillTreePurchaseStore("test").purchase(connection, 7, 9, "tree", "node", 0, 5, 2, amount, skill, 0, commit)
        }
    }

    /** 创建隔离内存数据库，表结构对应通用字段。 */
    private fun database(): Connection {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        execute(connection, "CREATE TABLE test_router(id INTEGER PRIMARY KEY, sp_current INTEGER, sp_used INTEGER)")
        execute(connection, "CREATE TABLE test_skill_tree_node(id INTEGER PRIMARY KEY AUTOINCREMENT, route INTEGER, tree TEXT, node TEXT, level INTEGER)")
        execute(connection, "CREATE TABLE test_skill(id INTEGER PRIMARY KEY AUTOINCREMENT, route INTEGER, node TEXT, level INTEGER)")
        execute(connection, "INSERT INTO test_router VALUES (7, 5, 2)")
        return connection
    }

    /** 执行固定 fixture SQL，不读取生产配置或玩家数据。 */
    private fun execute(connection: Connection, sql: String) {
        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    /** 查询事务落盘后的单个整数结果。 */
    private fun scalar(connection: Connection, sql: String): Int {
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                assertTrue(rows.next())
                return rows.getInt(1)
            }
        }
    }

    /** 核对失败路径中资源、当前 SP、已用 SP、节点和技能均未新增消费。 */
    private fun assertUnchanged(connection: Connection, cost: Resource) {
        assertEquals(3, cost.remaining)
        assertEquals(5, scalar(connection, "SELECT sp_current FROM test_router"))
        assertEquals(2, scalar(connection, "SELECT sp_used FROM test_router"))
        assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM test_skill_tree_node"))
        assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM test_skill"))
    }

    /** 可注入提交及回滚失败的外部资源边界，不模拟学习或数据库算法。 */
    private class Resource(initial: Int) : LearningCost {
        var remaining = initial
        var commits = 0
        var rollbacks = 0
        var failCommit = false
        var failRollback = false

        /** 本资源不产生技能点成本。 */
        override fun skillPoints(): Int {
            return 0
        }

        /** 模拟部分扣除后报错的外部宿主。 */
        override fun commit() {
            commits += 1
            remaining -= 1
            if (failCommit) {
                error("resource commit failed")
            }
        }

        /** 恢复已扣除的资源，并允许测试独立注入回滚失败。 */
        override fun rollback() {
            rollbacks += 1
            if (failRollback) {
                error("resource rollback failed")
            }
            if (commits > 0) {
                remaining += 1
            }
        }
    }
}
