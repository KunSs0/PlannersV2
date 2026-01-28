package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import taboolib.common.platform.function.getDataFolder
import taboolib.common.util.unsafeLazy
import taboolib.common5.clong
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
        add("route") { type(ColumnTypeSQLite.INTEGER) }
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
    }

    init {
        tableUser.createTable(dataSource)
        tableRoute.createTable(dataSource)
        tableMetadata.createTable(dataSource)
        tableSkill.createTable(dataSource)
    }

    override fun getPlayerProfile(player: Player): PlayerTemplate {
        val route = getRoute(player)
        val metadataMap = getMetadataMap(player)
        return PlayerTemplate(getUserId(player).id, player, route, metadataMap)
    }

    override fun updateRoute(template: PlayerTemplate) {
        val userId = getUserId(template.onlinePlayer).id
        tableUser.update(dataSource) {
            where { "id" eq userId }
            set("route", template.route?.bindingId)
        }
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
        return nativeUserId(player).also {
            this.cachedId[player.uniqueId] = it.id
        }
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

    private fun getRoute(player: Player): PlayerRoute? {
        val userId = getUserId(player)
        if (userId.created) {
            return null
        }

        return tableUser.select(dataSource) {
            where { "id" eq userId.id }
            rows("route")
        }.firstOrNull {
            val id = getObject("route")?.clong ?: return@firstOrNull null
            getRouteById(id)
        }
    }

    private fun getPlayerSkills(route: Long): List<PlayerSkill> {
        return tableSkill.select(dataSource) {
            where { "route" eq route }
            rows("id", "node", "level", "binding")
        }.map { PlayerSkill(getLong("id"), getString("node"), getInt("level"), getString("binding")) }
    }

    private fun getRouteById(id: Long): PlayerRoute {
        return tableRoute.select(dataSource) {
            where { "id" eq id }
            rows("id", "router", "parent", "route")
        }.first {
            PlayerRoute(
                getLong("id"),
                getString("router"),
                PlayerRoute.Node(getLong("parent"), getString("route")),
                getPlayerSkills(id)
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

    override fun createPlayerSkill(template: PlayerTemplate, skill: Skill): CompletableFuture<PlayerSkill> {
        val future = CompletableFuture<PlayerSkill>()
        val route = template.route?.bindingId ?: error("Player ${template.onlinePlayer.name} not find route")
        if (skill is ImmutableSkill) {
            tableSkill.insert(dataSource, "route", "node", "level") {
                value(route, skill.id, skill.startedLevel)
                onFinally {
                    val id = getId(generatedKeys)
                    future.complete(PlayerSkill(id, skill.id, skill.startedLevel, null))
                }
            }
        } else if (skill is PlayerSkill && skill.index == -1L) {
            tableSkill.nullableInsert(dataSource) {
                set("route", route)
                set("node", skill.id)
                set("level", skill.level)
                set("binding", skill.binding?.id)
                onFinally {
                    skill.index = getId(generatedKeys)
                    future.complete(skill)
                }
            }
        } else {
            future.complete(null)
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
            set("binding", skill.binding?.id)
        }
    }

    override fun createPlayerJob(
        template: PlayerTemplate,
        parentId: Long,
        route: ImmutableRoute
    ): CompletableFuture<PlayerRoute> {
        val future = CompletableFuture<PlayerRoute>()
        val node = PlayerRoute.Node(parentId, route.id)
        tableRoute.insert(dataSource, "user", "router", "parent", "route") {
            value(template.id, route.routerId, node.parentId, node.route)
            onFinally {
                future.complete(PlayerRoute(getId(generatedKeys), route.routerId, node, emptyList()))
            }
        }
        return future
    }

    override fun createPlayerJob(template: PlayerTemplate, route: ImmutableRoute): CompletableFuture<PlayerRoute> {
        return createPlayerJob(template, template.route?.bindingId ?: -1L, route)
    }

    private fun getId(resultSet: ResultSet): Long {
        resultSet.next()
        return resultSet.getLong(1)
    }

    class Id(val id: Long, val created: Boolean)
}
