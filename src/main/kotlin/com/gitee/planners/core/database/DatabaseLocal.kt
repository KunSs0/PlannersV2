package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import org.bukkit.entity.Player
import taboolib.common.platform.function.getDataFolder
import taboolib.common.util.unsafeLazy
import taboolib.module.database.*
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class DatabaseLocal : Database {

    val host = HostSQLite(File(getDataFolder(), "data.db"))

    /** 写入器只由数据库队列读取，初始化不会发生在业务主线程。 */
    override val store: ProfileStore
        get() = ProfileStore(dataSource, "planners")

    val cachedId = mutableMapOf<UUID, Long>()

    val dataSource by unsafeLazy {
        host.createDataSource()
    }

    val tableUser = Table("planners_user", host) {
        add { id() }
        add("user") { type(ColumnTypeSQLite.TEXT) }
    }

    val tableRoute = Table("planners_route", host) {
        add { id() }
        add("user") { type(ColumnTypeSQLite.INTEGER) }
        add("router") { type(ColumnTypeSQLite.TEXT) }
        add("parent") { type(ColumnTypeSQLite.INTEGER) }
        add("route") { type(ColumnTypeSQLite.TEXT) }
    }

    val tableMetadata = Table("planners_metadata", host) {
        add { id() }
        add("user") { type(ColumnTypeSQLite.INTEGER) }
        add("node") { type(ColumnTypeSQLite.TEXT) }
        add("type") { type(ColumnTypeSQLite.TEXT) }
        add("token") { type(ColumnTypeSQLite.TEXT) }
        add("stop_time") { type(ColumnTypeSQLite.INTEGER) }
    }

    val tableSkill = Table("planners_skill", host) {
        add { id() }
        add("route") { type(ColumnTypeSQLite.INTEGER) }
        add("node") { type(ColumnTypeSQLite.TEXT) }
        add("level") { type(ColumnTypeSQLite.INTEGER) }
        add("binding") { type(ColumnTypeSQLite.TEXT) }
        add("equipped") { type(ColumnTypeSQLite.INTEGER) }
        add("backpack_page") { type(ColumnTypeSQLite.TEXT) }
        add("backpack_slot") { type(ColumnTypeSQLite.TEXT) }
    }

    val tableSkillTreeNode = Table("planners_skill_tree_node", host) {
        add { id() }
        add("route") { type(ColumnTypeSQLite.INTEGER) }
        add("tree") { type(ColumnTypeSQLite.TEXT) }
        add("node") { type(ColumnTypeSQLite.TEXT) }
        add("level") { type(ColumnTypeSQLite.INTEGER) }
    }

    val tableRouter = Table("planners_router", host) {
        add { id() }
        add("user") { type(ColumnTypeSQLite.INTEGER) }
        add("router") { type(ColumnTypeSQLite.TEXT) }
        add("level") { type(ColumnTypeSQLite.INTEGER) }
        add("experience") { type(ColumnTypeSQLite.INTEGER) }
        add("current_route") { type(ColumnTypeSQLite.INTEGER) }
        add("sp_current") { type(ColumnTypeSQLite.INTEGER) }
        add("sp_used") { type(ColumnTypeSQLite.INTEGER) }
    }

    init {
        migrateLegacyRouterSchema()
        tableUser.createTable(dataSource)
        tableRoute.createTable(dataSource)
        tableMetadata.createTable(dataSource)
        tableSkill.createTable(dataSource)
        tableSkillTreeNode.createTable(dataSource)
        tableRouter.createTable(dataSource)
    }

    /**
     * 将职业线重构前的本地数据迁移至 PlayerRouter 聚合模型。
     *
     * 旧模型以 planners_user.route 保存当前路线，并将 SP 保存在路线记录中；
     * 新模型将这些状态统一保存至 planners_router。仅接受每个玩家一条且与当前路线一致的
     * Router 记录，避免在迁移时静默删除旧模型中不可判定的历史职业数据。
     */
    private fun migrateLegacyRouterSchema() {
        val connection = dataSource.connection
        try {
            val routerColumns = getTableColumns(connection, "planners_router")
            if (routerColumns.isEmpty()) {
                return
            }
            val hasCurrentRoute = routerColumns.contains("current_route")
            val hasSkillPointsCurrent = routerColumns.contains("sp_current")
            val hasSkillPointsUsed = routerColumns.contains("sp_used")
            if (hasCurrentRoute && hasSkillPointsCurrent && hasSkillPointsUsed) {
                return
            }

            val userColumns = getTableColumns(connection, "planners_user")
            val routeColumns = getTableColumns(connection, "planners_route")
            if (!userColumns.contains("route") || !routeColumns.contains("sp_current") || !routeColumns.contains("sp_used")) {
                error("无法迁移 Planners 本地职业数据：旧表缺少当前路线或技能点字段")
            }

            validateLegacyRouterData(connection)
            val originalAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                val statement = connection.createStatement()
                try {
                    if (!hasCurrentRoute) {
                        statement.executeUpdate("ALTER TABLE planners_router ADD COLUMN current_route INTEGER NOT NULL DEFAULT -1")
                    }
                    if (!hasSkillPointsCurrent) {
                        statement.executeUpdate("ALTER TABLE planners_router ADD COLUMN sp_current INTEGER NOT NULL DEFAULT 0")
                    }
                    if (!hasSkillPointsUsed) {
                        statement.executeUpdate("ALTER TABLE planners_router ADD COLUMN sp_used INTEGER NOT NULL DEFAULT 0")
                    }
                    statement.executeUpdate(
                        """
                        UPDATE planners_router
                        SET current_route = (
                            SELECT planners_user.route
                            FROM planners_user
                            WHERE planners_user.id = planners_router.user
                        ),
                        sp_current = COALESCE((
                            SELECT planners_route.sp_current
                            FROM planners_route
                            WHERE planners_route.id = (
                                SELECT planners_user.route
                                FROM planners_user
                                WHERE planners_user.id = planners_router.user
                            )
                        ), 0),
                        sp_used = COALESCE((
                            SELECT planners_route.sp_used
                            FROM planners_route
                            WHERE planners_route.id = (
                                SELECT planners_user.route
                                FROM planners_user
                                WHERE planners_user.id = planners_router.user
                            )
                        ), 0)
                        """.trimIndent()
                    )
                    connection.commit()
                } finally {
                    statement.close()
                }
            } catch (exception: Throwable) {
                connection.rollback()
                throw exception
            } finally {
                connection.autoCommit = originalAutoCommit
            }
        } finally {
            connection.close()
        }
    }

    /** 验证旧模型的数据可以无歧义地迁移为一个 PlayerRouter。 */
    private fun validateLegacyRouterData(connection: Connection) {
        val statement = connection.createStatement()
        try {
            val invalidRouter = statement.executeQuery(
                """
                SELECT planners_router.id AS router_id
                FROM planners_router
                LEFT JOIN planners_user ON planners_user.id = planners_router.user
                LEFT JOIN planners_route ON planners_route.id = planners_user.route
                WHERE planners_user.route IS NULL
                   OR planners_route.id IS NULL
                   OR planners_route.user != planners_router.user
                   OR planners_route.router != planners_router.router
                """.trimIndent()
            )
            try {
                if (invalidRouter.next()) {
                    val routerId = invalidRouter.getLong("router_id")
                    error("无法迁移 Planners 本地职业数据：Router $routerId 不对应唯一的当前职业路线")
                }
            } finally {
                invalidRouter.close()
            }

            val duplicatedRouter = statement.executeQuery(
                """
                SELECT user, COUNT(*) AS router_count
                FROM planners_router
                GROUP BY user
                HAVING COUNT(*) > 1
                """.trimIndent()
            )
            try {
                if (duplicatedRouter.next()) {
                    val userId = duplicatedRouter.getLong("user")
                    error("无法迁移 Planners 本地职业数据：玩家 $userId 存在多个 Router 记录")
                }
            } finally {
                duplicatedRouter.close()
            }
        } finally {
            statement.close()
        }
    }

    /** 返回指定 SQLite 表已存在的列名。 */
    private fun getTableColumns(connection: Connection, tableName: String): Set<String> {
        val columns = mutableSetOf<String>()
        val resultSet = connection.metaData.getColumns(null, null, tableName, null)
        try {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME").lowercase())
            }
        } finally {
            resultSet.close()
        }
        return columns
    }

    override fun getPlayerProfile(player: Player): PlayerTemplate {
        check(!org.bukkit.Bukkit.isPrimaryThread()) { "主线程禁止读取数据库档案" }
        val playerRouter = getPlayerRouter(player)
        val metadataMap = getMetadataMap(player)
        return PlayerTemplate(getUserId(player).id, player, playerRouter, metadataMap)
    }

    private fun getMetadataMap(player: Player): Map<String, Metadata> {
        val userId = getUserId(player).id
        return tableMetadata.select(dataSource) {
            where { "user" eq userId }
            rows("node", "type", "token", "stop_time")
        }.map {
            val type = Class.forName(getString("type"))
            val token = getString("token")
            val timeoutTick = getLong("stop_time")
            getString("node") to Metadata.Loader.parseTypeToken(type, token, timeoutTick)
        }.toMap()
    }

    private fun getUserId(player: Player): Id {
        if (this.cachedId.containsKey(player.uniqueId)) {
            return Id(this.cachedId[player.uniqueId]!!, false)
        }
        val userId = nativeUserId(player)
        this.cachedId[player.uniqueId] = userId.id
        return userId
    }

    private fun nativeUserId(player: Player): Id {
        return if (tableUser.find(dataSource) { where { "user" eq player.uniqueId.toString() } }) {
            val id = tableUser.select(dataSource) {
                where { "user" eq player.uniqueId.toString() }
                rows("id")
            }.first { getLong("id") }
            Id(id, false)
        } else {
            Id(createUserId(player), true)
        }
    }

    private fun createUserId(player: Player): Long {
        var createdId: Long? = null
        tableUser.insert(dataSource, "user") {
            value(player.uniqueId.toString())
            onFinally {
                createdId = getId(generatedKeys)
            }
        }
        return checkNotNull(createdId) { "用户插入未返回生成主键" }
    }

    private fun getPlayerRouter(player: Player): PlayerRouter? {
        val userId = getUserId(player)
        if (userId.created) {
            return null
        }

        val routerIds = tableRouter.select(dataSource) {
            where { "user" eq userId.id }
            rows("router")
        }.map {
            getString("router")
        }
        if (routerIds.isEmpty()) {
            return null
        }
        if (routerIds.size > 1) {
            error("玩家 '${player.uniqueId}' 存在多个 PlayerRouter")
        }
        return loadPlayerRouter(userId.id, routerIds[0])
    }

    private fun getPlayerSkills(route: Long): List<PlayerSkill> {
        return tableSkill.select(dataSource) {
            where { "route" eq route }
            rows("id", "node", "level", "binding", "equipped", "backpack_page", "backpack_slot")
        }.map {
            PlayerSkill(
                getLong("id"), getString("node"), getInt("level"),
                getInt("equipped") != 0, getString("backpack_page"), getString("backpack_slot")
            )
        }
    }

    private fun getSkillTreeNodeStates(route: Long): List<PlayerSkillTreeNodeState> {
        return tableSkillTreeNode.select(dataSource) {
            where { "route" eq route }
            rows("id", "tree", "node", "level")
        }.map {
            PlayerSkillTreeNodeState(
                getLong("id"),
                getString("tree"),
                getString("node"),
                getInt("level")
            )
        }
    }

    private fun getPlayerRoutes(userId: Long, routerId: String): List<PlayerRoute> {
        return tableRoute.select(dataSource) {
            where {
                "user" eq userId
                "router" eq routerId
            }
            rows("id", "router", "parent", "route")
        }.map {
            val routeId = getLong("id")
            PlayerRoute(
                routeId,
                getString("router"),
                getLong("parent"),
                getString("route"),
                getPlayerSkills(routeId),
                getSkillTreeNodeStates(routeId)
            )
        }
    }

    private fun loadPlayerRouter(userId: Long, routerId: String): PlayerRouter? {
        return tableRouter.select(dataSource) {
            where {
                "user" eq userId
                "router" eq routerId
            }
            rows("id", "level", "experience", "current_route", "sp_current", "sp_used")
        }.firstOrNull {
            PlayerRouter(
                getLong("id"),
                userId,
                routerId,
                getInt("level"),
                getInt("experience"),
                getLong("current_route"),
                getInt("sp_current"),
                getInt("sp_used"),
                getPlayerRoutes(userId, routerId)
            )
        }
    }

    private fun getId(resultSet: ResultSet): Long {
        resultSet.next()
        return resultSet.getLong(1)
    }

    class Id(val id: Long, val created: Boolean)
}
