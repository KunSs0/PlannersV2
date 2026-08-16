package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import taboolib.common.platform.function.getDataFolder
import taboolib.common.util.unsafeLazy
import taboolib.module.database.*
import java.io.File
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.CompletableFuture

class DatabaseLocal : Database {

    val host = HostSQLite(File(getDataFolder(), "data.db"))

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
        tableUser.createTable(dataSource)
        tableRoute.createTable(dataSource)
        tableMetadata.createTable(dataSource)
        tableSkill.createTable(dataSource)
        tableRouter.createTable(dataSource)
    }

    override fun getPlayerProfile(player: Player): PlayerTemplate {
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
            Id(createUserId(player).get(), true)
        }
    }

    private fun createUserId(player: Player): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        tableUser.insert(dataSource, "user") {
            value(player.uniqueId.toString())
            onFinally {
                future.complete(getId(generatedKeys))
            }
        }
        return future
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
                getPlayerSkills(routeId)
            )
        }
    }

    override fun updateMetadata(template: PlayerTemplate, id: String, metadata: Metadata) {
        if (metadata is MetadataTypeToken.Void || metadata.isTimeout()) {
            tableMetadata.delete(dataSource) { whereWithMetadata(template, id) }
        } else if (tableMetadata.find(dataSource) { whereWithMetadata(template, id) }) {
            tableMetadata.update(dataSource) {
                whereWithMetadata(template, id)
                set("type", metadata.clazz.name)
                set("token", Metadata.Loader.toJson(metadata))
                set("stop_time", metadata.timeoutTick)
            }
        } else {
            tableMetadata.nullableInsert(dataSource) {
                set("user", template.id)
                set("node", id)
                set("type", metadata.clazz.name)
                set("token", Metadata.Loader.toJson(metadata))
                set("stop_time", metadata.timeoutTick)
            }
        }
    }

    fun ActionFilterable.whereWithMetadata(template: PlayerTemplate, id: String) {
        return where {
            "user" eq template.id
            "node" eq id
        }
    }

    override fun createPlayerSkill(
        template: PlayerTemplate,
        route: PlayerRoute,
        skill: ImmutableSkill
    ): CompletableFuture<PlayerSkill> {
        val future = CompletableFuture<PlayerSkill>()
        tableSkill.insert(dataSource, "route", "node", "level") {
            value(route.bindingId, skill.id, skill.startedLevel)
            onFinally {
                val id = getId(generatedKeys)
                future.complete(PlayerSkill(id, skill.id, skill.startedLevel))
            }
        }
        return future
    }

    override fun deleteSkill(vararg skill: PlayerSkill) {
        tableSkill.delete(dataSource) {
            where { "id" inside skill.map { it.index }.toTypedArray() }
        }
    }

    override fun updateSkill(skill: PlayerSkill) {
        tableSkill.update(dataSource) {
            where { "id" eq skill.index }
            set("level", skill.level)
            set("equipped", if (skill.equipped) 1 else 0)
            set("backpack_page", skill.backpackPage)
            set("backpack_slot", skill.backpackSlot)
        }
    }

    override fun createPlayerRoute(
        router: PlayerRouter,
        parentId: Long,
        route: ImmutableRoute
    ): CompletableFuture<PlayerRoute> {
        val future = CompletableFuture<PlayerRoute>()
        tableRoute.insert(dataSource, "user", "router", "parent", "route") {
            value(router.userId, route.routerId, parentId, route.id)
            onFinally {
                future.complete(PlayerRoute(getId(generatedKeys), route.routerId, parentId, route.id, emptyList()))
            }
        }
        return future
    }

    override fun loadPlayerRouter(userId: Long, routerId: String): PlayerRouter? {
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

    override fun createPlayerRouter(userId: Long, routerId: String, initialLevel: Int): PlayerRouter {
        val future = CompletableFuture<PlayerRouter>()
        tableRouter.insert(
            dataSource,
            "user",
            "router",
            "level",
            "experience",
            "current_route",
            "sp_current",
            "sp_used"
        ) {
            value(userId, routerId, initialLevel, 0, -1L, 0, 0)
            onFinally {
                val id = getId(generatedKeys)
                future.complete(PlayerRouter(id, userId, routerId, initialLevel, 0, -1L, 0, 0, emptyList()))
            }
        }
        return future.get()
    }

    override fun updatePlayerRouter(router: PlayerRouter) {
        tableRouter.update(dataSource) {
            where { "id" eq router.bindingId }
            set("level", router.level)
            set("experience", router.experience)
            set("current_route", router.currentRouteId)
            set("sp_current", router.skillPointsCurrent)
            set("sp_used", router.skillPointsUsed)
        }
    }

    override fun deletePlayerRouter(router: PlayerRouter) {
        val routes = getPlayerRoutes(router.userId, router.routerId)
        val routeIds = routes.map { it.bindingId }
        if (routeIds.isNotEmpty()) {
            tableSkill.delete(dataSource) {
                where { "route" inside routeIds.toTypedArray() }
            }
        }
        tableRoute.delete(dataSource) {
            where {
                "user" eq router.userId
                "router" eq router.routerId
            }
        }
        tableRouter.delete(dataSource) {
            where { "id" eq router.bindingId }
        }
    }

    private fun getId(resultSet: ResultSet): Long {
        resultSet.next()
        return resultSet.getLong(1)
    }

    class Id(val id: Long, val created: Boolean)
}
