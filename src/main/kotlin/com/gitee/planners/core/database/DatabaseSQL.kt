package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import org.bukkit.entity.Player
import taboolib.common.util.unsafeLazy
import taboolib.module.database.*
import java.sql.ResultSet
import java.util.UUID

class DatabaseSQL : Database {

    val host = HostSQL(Database.option.get().sql!!)

    val prefix: String
        get() = Database.option.get().sql!!.getString("table", "planners_v2")!!

    /** 写入器只由数据库队列读取，初始化不会发生在业务主线程。 */
    override val store: ProfileStore
        get() = ProfileStore(dataSource, prefix)

    val cachedId = mutableMapOf<UUID, Long>()

    val dataSource by unsafeLazy {
        host.createDataSource()
    }

    val tableUser = Table("${prefix}_user", host) {
        add { id() }
        add("user") { type(ColumnTypeSQL.VARCHAR, 36) }
    }

    val tableRoute = Table("${prefix}_route", host) {
        add { id() }
        add("user") { type(ColumnTypeSQL.INT) }
        add("router") { type(ColumnTypeSQL.VARCHAR, 60) }
        add("parent") { type(ColumnTypeSQL.INT) }
        add("route") { type(ColumnTypeSQL.VARCHAR, 60) }
    }

    val tableMetadata = Table("${prefix}_metadata", host) {
        add { id() }
        add("user") { type(ColumnTypeSQL.INT) }
        add("node") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("type") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("token") { type(ColumnTypeSQL.VARCHAR, 255) }
        add("stop_time") { type(ColumnTypeSQL.BIGINT) }
    }

    val tableSkill = Table("${prefix}_skill", host) {
        add { id() }
        add("route") { type(ColumnTypeSQL.INT) }
        add("node") { type(ColumnTypeSQL.VARCHAR, 60) }
        add("level") { type(ColumnTypeSQL.INT) }
        add("binding") { type(ColumnTypeSQL.VARCHAR, 60) }
        add("equipped") { type(ColumnTypeSQL.INT) }
        add("backpack_page") { type(ColumnTypeSQL.VARCHAR, 60) }
        add("backpack_slot") { type(ColumnTypeSQL.VARCHAR, 60) }
    }

    val tableSkillTreeNode = Table("${prefix}_skill_tree_node", host) {
        add { id() }
        add("route") { type(ColumnTypeSQL.INT) }
        add("tree") { type(ColumnTypeSQL.VARCHAR, 60) }
        add("node") { type(ColumnTypeSQL.VARCHAR, 60) }
        add("level") { type(ColumnTypeSQL.INT) }
    }

    val tableRouter = Table("${prefix}_router", host) {
        add { id() }
        add("user") { type(ColumnTypeSQL.INT) }
        add("router") { type(ColumnTypeSQL.VARCHAR, 60) }
        add("level") { type(ColumnTypeSQL.INT) }
        add("experience") { type(ColumnTypeSQL.INT) }
        add("current_route") { type(ColumnTypeSQL.INT) }
        add("sp_current") { type(ColumnTypeSQL.INT) }
        add("sp_used") { type(ColumnTypeSQL.INT) }
    }

    init {
        tableUser.createTable(dataSource)
        tableRoute.createTable(dataSource)
        tableMetadata.createTable(dataSource)
        tableSkill.createTable(dataSource)
        tableSkillTreeNode.createTable(dataSource)
        tableRouter.createTable(dataSource)
    }

    // 该方法最好运行在异步 否则向数据库插入数据时会耗时
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
        // 添加到map 由于id是唯一的 不用在join和quit维护id
        val id = nativeUserId(player)
        this.cachedId[player.uniqueId] = id.id
        return id
    }

    private fun nativeUserId(player: Player): Id {
        return if (tableUser.find(dataSource) { where { "user" eq player.uniqueId.toString() } }) {
            val id = tableUser.select(dataSource) {
                where { "user" eq player.uniqueId.toString() }
                rows("id")
            }.first { getLong("id") }
            Id(id, false)
        }
        // 新建用户id
        else {
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
