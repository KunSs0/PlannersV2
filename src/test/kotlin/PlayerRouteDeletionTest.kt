import com.gitee.planners.core.database.Database
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.sqlite.SQLiteDataSource
import taboolib.module.database.Table
import java.io.File
import java.sql.Connection
import java.util.UUID

/**
 * 以合成内存数据库执行共用 JDBC 写入器的真实删除方法，验证旧路由身份隔离。
 */
class PlayerRouteDeletionTest {
    /**
     * 本地和 SQL 共用的写入器只能删除旧记录，保留同玩家重新创建的同名职业及其他玩家。
     */
    @Test
    fun delayedDeletionPreservesReselectedProfessionForBothProviders() {
        // 使用真实 Table 删除语句，只替换数据库连接和无关的构造初始化。
        for (prefix in listOf("planners", "custom")) {
            val dataSource = SQLiteDataSource()
            dataSource.url = "jdbc:sqlite:file:route-clear-${UUID.randomUUID()}?mode=memory&cache=shared"
            val anchor = dataSource.connection
            try {
                execute(anchor, "CREATE TABLE ${prefix}_router(id INTEGER, user INTEGER, router TEXT)")
                execute(anchor, "CREATE TABLE ${prefix}_route(id INTEGER, user INTEGER, router TEXT)")
                execute(anchor, "CREATE TABLE ${prefix}_skill(id INTEGER, route INTEGER)")
                execute(anchor, "CREATE TABLE ${prefix}_skill_tree_node(id INTEGER, route INTEGER)")
                execute(anchor, "INSERT INTO ${prefix}_router VALUES (42,1,'job'),(43,1,'job'),(44,2,'job')")
                execute(anchor, "INSERT INTO ${prefix}_route VALUES (11,1,'job'),(12,1,'job'),(13,2,'job')")
                execute(anchor, "INSERT INTO ${prefix}_skill VALUES (101,11),(102,12),(103,13)")
                execute(anchor, "INSERT INTO ${prefix}_skill_tree_node VALUES (201,11),(202,12),(203,13)")
                val store = com.gitee.planners.core.database.ProfileStore(dataSource, prefix)
                val deletion = com.gitee.planners.core.database.ProfileSnapshot.Deletion(
                    com.gitee.planners.core.database.RecordIdentity(42),
                    listOf(com.gitee.planners.core.database.RecordIdentity(11)))
                store.deleteRouter(deletion)
                assertEquals(0, count(anchor, "${prefix}_router", "id=42"))
                assertEquals(0, count(anchor, "${prefix}_route", "id=11"))
                assertEquals(0, count(anchor, "${prefix}_skill", "route=11"))
                assertEquals(0, count(anchor, "${prefix}_skill_tree_node", "route=11"))
                for (routeId in listOf(12, 13)) {
                    assertEquals(1, count(anchor, "${prefix}_route", "id=$routeId"))
                    assertEquals(1, count(anchor, "${prefix}_skill", "route=$routeId"))
                    assertEquals(1, count(anchor, "${prefix}_skill_tree_node", "route=$routeId"))
                }
                assertEquals(1, count(anchor, "${prefix}_router", "id=43"))
                assertEquals(1, count(anchor, "${prefix}_router", "id=44"))
                // 同一删除任务重复运行也不得触及新职业。
                store.deleteRouter(deletion)
                assertEquals(1, count(anchor, "${prefix}_router", "id=43"))
                assertEquals(1, count(anchor, "${prefix}_route", "id=12"))
            } finally {
                anchor.close()
            }
        }
    }

    /**
     * 执行仅作用于本测试内存数据库的建表或造数 SQL。
     * @param connection 测试连接。
     * @param sql 固定的合成 SQL。
     */
    private fun execute(connection: Connection, sql: String) {
        // 显式关闭语句，不持有实际玩家数据库连接。
        val statement = connection.createStatement()
        try {
            statement.execute(sql)
        } finally {
            statement.close()
        }
    }

    /**
     * 查询测试表中满足固定条件的记录数。
     * @param connection 测试连接。
     * @param table 合成表名。
     * @param condition 合成记录身份条件。
     * @return 剩余记录数。
     */
    private fun count(connection: Connection, table: String, condition: String): Int {
        // 条件全部由测试固定生成，不接受外部输入。
        val statement = connection.createStatement()
        try {
            val rows = statement.executeQuery("SELECT COUNT(*) FROM $table WHERE $condition")
            try {
                assertTrue(rows.next())
                return rows.getInt(1)
            } finally {
                rows.close()
            }
        } finally {
            statement.close()
        }
    }
}
