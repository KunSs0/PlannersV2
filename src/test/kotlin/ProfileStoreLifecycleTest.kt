import com.gitee.planners.core.database.ProfileSnapshot
import com.gitee.planners.core.database.ProfileStore
import com.gitee.planners.core.database.RecordIdentity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.sqlite.SQLiteDataSource
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

/** 使用真实 SQL 验证临时身份的完整持久化生命周期及旧新职业隔离。 */
class ProfileStoreLifecycleTest {
    /** 新选职、转职、技能创建和装备保存解析生成主键，但运行时身份不改变。 */
    @Test
    fun resolvesGeneratedIdsWithoutChangingRuntimeIdentities() {
        Fixture().use { fixture ->
            val routerId = RecordIdentity(RecordIdentity.next())
            val first = RecordIdentity(RecordIdentity.next())
            val second = RecordIdentity(RecordIdentity.next())
            val initial = fixture.router(routerId, first, 2)
            fixture.store.createRoute(initial, ProfileSnapshot.Route(first, 1, "job", null, "first"), true)
            fixture.store.createRoute(fixture.router(routerId, second, 2), ProfileSnapshot.Route(second, 1, "job", first, "second"), false)
            val skillId = RecordIdentity(RecordIdentity.next())
            fixture.store.createSkill(second, ProfileSnapshot.Skill(skillId, "skill", 1, false, null, null))
            fixture.store.saveSkill(ProfileSnapshot.Skill(skillId, "skill", 2, true, "main", "one"))
            fixture.store.saveRouter(fixture.router(routerId, second, 5))
            assertEquals(second.persisted(), fixture.scalar("SELECT current_route FROM test_router"))
            assertEquals(first.persisted(), fixture.scalar("SELECT parent FROM test_route WHERE route='second'"))
            assertEquals(second.persisted(), fixture.scalar("SELECT route FROM test_skill"))
            assertEquals(2, fixture.scalar("SELECT level FROM test_skill"))
            assertEquals(1, fixture.scalar("SELECT equipped FROM test_skill"))
            assertEquals(5, fixture.scalar("SELECT sp_current FROM test_router"))
            assertTrue(routerId.runtimeId < -1 && second.runtimeId < -1 && skillId.runtimeId < -1)
        }
    }

    /** 选职事务的阶段插入失败时回滚职业线，不向后继 SQL 发布半成品身份。 */
    @Test
    fun initialRouteFailureRollsBackWholeProfessionAndGeneratedIds() {
        Fixture().use { fixture ->
            fixture.execute("CREATE TRIGGER fail_route BEFORE INSERT ON test_route BEGIN SELECT RAISE(ABORT, 'route failed'); END")
            val router = RecordIdentity(RecordIdentity.next())
            val route = RecordIdentity(RecordIdentity.next())
            assertThrows(SQLException::class.java) {
                fixture.store.createRoute(fixture.router(router, route, 2), ProfileSnapshot.Route(route, 1, "job", null, "first"), true)
            }
            assertEquals(0, fixture.scalar("SELECT COUNT(*) FROM test_router"))
            assertEquals(0, fixture.scalar("SELECT COUNT(*) FROM test_route"))
            assertThrows(IllegalStateException::class.java) { router.persisted() }
            assertThrows(IllegalStateException::class.java) { route.persisted() }
        }
    }

    /** 已创建、保存、删除旧职业后再选同名职业，不会删除新记录。 */
    @Test
    fun clearAndReselectSameProfessionRemainIsolated() {
        Fixture().use { fixture ->
            val oldRouter = RecordIdentity(RecordIdentity.next())
            val oldRoute = RecordIdentity(RecordIdentity.next())
            fixture.store.createRoute(fixture.router(oldRouter, oldRoute, 2), ProfileSnapshot.Route(oldRoute, 1, "job", null, "first"), true)
            fixture.store.deleteRouter(ProfileSnapshot.Deletion(oldRouter, listOf(oldRoute)))
            val nextRouter = RecordIdentity(RecordIdentity.next())
            val nextRoute = RecordIdentity(RecordIdentity.next())
            fixture.store.createRoute(fixture.router(nextRouter, nextRoute, 2), ProfileSnapshot.Route(nextRoute, 1, "job", null, "first"), true)
            fixture.store.deleteRouter(ProfileSnapshot.Deletion(oldRouter, listOf(oldRoute)))
            assertEquals(1, fixture.scalar("SELECT COUNT(*) FROM test_router"))
            assertEquals(nextRouter.persisted(), fixture.scalar("SELECT id FROM test_router"))
            assertEquals(nextRoute.persisted(), fixture.scalar("SELECT id FROM test_route"))
        }
    }

    /** 元数据依次保存替换和删除，同一用户字段不会产生重复记录。 */
    @Test
    fun metadataSavesLatestCapturedValueAndDeletion() {
        Fixture().use { fixture ->
            fixture.store.saveMetadata(ProfileSnapshot.Metadata(1, "page", "String", "old", 0))
            fixture.store.saveMetadata(ProfileSnapshot.Metadata(1, "page", "String", "new", 0))
            assertEquals(1, fixture.scalar("SELECT COUNT(*) FROM test_metadata WHERE token='new'"))
            assertEquals(1, fixture.scalar("SELECT COUNT(*) FROM test_metadata"))
            fixture.store.saveMetadata(ProfileSnapshot.Metadata(1, "page", null, null, 0))
            assertEquals(0, fixture.scalar("SELECT COUNT(*) FROM test_metadata"))
        }
    }

    /** 完整字段的合成 SQLite，不连接任何实际玩家数据库。 */
    private class Fixture : AutoCloseable {
        private val source = SQLiteDataSource()
        private val anchor: Connection
        val store: ProfileStore

        init {
            source.url = "jdbc:sqlite:file:lifecycle-${UUID.randomUUID()}?mode=memory&cache=shared"
            anchor = source.connection
            store = ProfileStore(source, "test")
            execute("CREATE TABLE test_router(id INTEGER PRIMARY KEY AUTOINCREMENT,user INTEGER,router TEXT,level INTEGER,experience INTEGER,current_route INTEGER,sp_current INTEGER,sp_used INTEGER)")
            execute("CREATE TABLE test_route(id INTEGER PRIMARY KEY AUTOINCREMENT,user INTEGER,router TEXT,parent INTEGER,route TEXT)")
            execute("CREATE TABLE test_skill(id INTEGER PRIMARY KEY AUTOINCREMENT,route INTEGER,node TEXT,level INTEGER,equipped INTEGER,backpack_page TEXT,backpack_slot TEXT)")
            execute("CREATE TABLE test_skill_tree_node(id INTEGER PRIMARY KEY AUTOINCREMENT,route INTEGER,tree TEXT,node TEXT,level INTEGER)")
            execute("CREATE TABLE test_metadata(id INTEGER PRIMARY KEY AUTOINCREMENT,user INTEGER,node TEXT,type TEXT,token TEXT,stop_time INTEGER)")
        }

        /** 根据稳定身份创建最终职业线快照。 */
        fun router(identity: RecordIdentity, current: RecordIdentity, points: Int): ProfileSnapshot.Router {
            return ProfileSnapshot.Router(identity, 1, "job", 1, 0, current, points, 0)
        }

        /** 执行本测试的固定建表和故障注入 SQL。 */
        fun execute(sql: String) {
            anchor.createStatement().use { statement -> statement.execute(sql) }
        }

        /** 验证真实写入器在独立连接提交的数据。 */
        fun scalar(sql: String): Long {
            source.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(sql).use { rows ->
                        assertTrue(rows.next())
                        return rows.getLong(1)
                    }
                }
            }
        }

        /** 关闭最后的锚点连接，销毁合成内存数据库。 */
        override fun close() {
            anchor.close()
        }
    }
}
