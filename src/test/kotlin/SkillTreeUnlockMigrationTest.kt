import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * 在独立内存 SQLite 上执行交付的完整迁移 SQL，不连接玩家数据库。
 */
class SkillTreeUnlockMigrationTest {
    /**
     * 已购 A/B 保留记录主键并映射到真实节点，未购记录归档后移除。
     */
    @Test
    fun migratesBothBranchesAndArchivesUnpurchasedRows() {
        val connection = database()
        try {
            execute(connection, "INSERT INTO planners_skill_tree_node VALUES (1, 10, 'knight_warder', 'knight-F-01', 1, 'A'), (2, 20, 'knight_warder', 'knight-F-01', 1, 'B'), (3, 30, 'knight_warder', 'knight-F-02', 0, NULL)")
            migrate(connection)
            val statement = connection.createStatement()
            try {
                val rows = statement.executeQuery("SELECT id, node, level FROM planners_skill_tree_node ORDER BY id")
                assertTrue(rows.next())
                assertEquals(1, rows.getInt("id"))
                assertEquals("knight-F-01_a", rows.getString("node"))
                assertEquals(1, rows.getInt("level"))
                assertTrue(rows.next())
                assertEquals("knight-F-01_b", rows.getString("node"))
                assertFalse(rows.next())
                rows.close()
                val archive = statement.executeQuery("SELECT COUNT(*) FROM planners_skill_tree_node_before_unlock_20261020")
                assertTrue(archive.next())
                assertEquals(3, archive.getInt(1))
                archive.close()
                val columns = statement.executeQuery("PRAGMA table_info(planners_skill_tree_node)")
                while (columns.next()) {
                    assertNotEquals("choice", columns.getString("name"))
                }
                columns.close()
            } finally {
                statement.close()
            }
        } finally {
            connection.close()
        }
    }

    /**
     * 缺失选项、未知选项、异常等级、重复与新旧并存均回滚，不推断选择。
     */
    @Test
    fun rejectsInvalidOrConflictingHistoricalRowsWithoutChangingThem() {
        val cases = listOf(
            "(1, 10, 'knight_warder', 'knight-F-01', NULL, NULL)",
            "(1, 10, 'knight_warder', 'knight-F-01', 1, NULL)",
            "(1, 10, 'knight_warder', 'knight-F-01', 1, 'C')",
            "(1, 10, 'knight_warder', 'knight-F-01', 2, 'A')",
            "(1, 10, 'knight_warder', 'knight-F-01', 0, 'A')",
            "(1, 10, 'knight_warder', 'knight-F-01', 1, 'A'), (2, 10, 'knight_warder', 'knight-F-01', 1, 'B')",
            "(1, 10, 'knight_warder', 'knight-F-01', 1, 'A'), (2, 10, 'knight_warder', 'knight-F-01_b', 1, NULL)"
        )
        for (values in cases) {
            val connection = database()
            try {
                execute(connection, "INSERT INTO planners_skill_tree_node VALUES $values")
                assertThrows(SQLException::class.java) {
                    migrate(connection)
                }
                val statement = connection.createStatement()
                try {
                    val rows = statement.executeQuery("SELECT node FROM planners_skill_tree_node WHERE id = 1")
                    assertTrue(rows.next())
                    assertEquals("knight-F-01", rows.getString(1))
                    rows.close()
                    val archive = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name = 'planners_skill_tree_node_before_unlock_20261020'")
                    assertTrue(archive.next())
                    assertEquals(0, archive.getInt(1))
                    archive.close()
                } finally {
                    statement.close()
                }
            } finally {
                connection.close()
            }
        }
    }

    /**
     * 创建测试专用旧表结构。
     *
     * @return 不包含任何实际玩家数据的内存连接。
     */
    private fun database(): Connection {
        // 明确使用内存数据库，禁止读取生产 data.db。
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        execute(connection, "CREATE TABLE planners_skill_tree_node (id INTEGER PRIMARY KEY, route INTEGER, tree TEXT, node TEXT, level INTEGER, choice TEXT)")
        return connection
    }

    /**
     * 按声明的语句边界执行真实迁移脚本，保留触发器整体语义。
     *
     * @param connection 当前测试数据库。
     */
    private fun migrate(connection: Connection) {
        val source = Files.readString(Path.of("migrations/20261020-unlock-nodes.sqlite.sql"))
        val statements = source.split("-- statement")
        for (index in 1 until statements.size) {
            execute(connection, statements[index])
        }
    }

    /**
     * 执行一条 SQL 并及时关闭 JDBC 语句。
     *
     * @param connection 数据库连接。
     * @param sql 完整 SQL 语句。
     */
    private fun execute(connection: Connection, sql: String) {
        val statement = connection.createStatement()
        try {
            statement.execute(sql)
        } finally {
            statement.close()
        }
    }
}
