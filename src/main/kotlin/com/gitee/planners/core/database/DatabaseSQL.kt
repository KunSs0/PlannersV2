package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.util.config
import org.bukkit.entity.Player
import taboolib.module.database.*
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.sql.DataSource

class DatabaseSQL : Database {

    val host = HostSQL(Database.option.get().sql!!)

    val cachedId = mutableMapOf<UUID, Long>()

    val dataSource: DataSource
        get() = host.createDataSource()

    val tableUser = Table("planners_user", host) {
        add { id() }
        add("user") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("route") { type(ColumnTypeSQL.INT) }
    }

    val tableRoute = Table("planners_route", host) {
        add { id() }
        add("router") { type(ColumnTypeSQL.VARCHAR, 10) }
        add("parent") { type(ColumnTypeSQL.VARCHAR, 10) }
        add("route") { type(ColumnTypeSQL.VARCHAR, 10) }
    }

    val tableMetadata = Table("planners_metadata", host) {
        add { id() }
        add("user") { type(ColumnTypeSQL.INT) }
        add("node") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("type") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("token") { type(ColumnTypeSQL.VARCHAR, 255) }
        add("stop_time") { type(ColumnTypeSQL.TIMESTAMP) }
    }

    val tableSkill = Table("planners_job", host) {
        add { id() }
        add("route") { type(ColumnTypeSQL.INT) }
        add("node") { type(ColumnTypeSQL.VARCHAR, 10) }
        add("level") { type(ColumnTypeSQL.INT) }
    }

    init {
        tableUser.createTable(dataSource)
        tableRoute.createTable(dataSource)
        tableMetadata.createTable(dataSource)
        tableSkill.createTable(dataSource)
    }

    // 该方法最好运行在异步 否则向数据库插入数据时会耗时
    override fun getPlayerProfile(player: Player): PlayerProfile {
        // 如果拿不到当前 route 则代表玩家还未选择 router
        val route = getRoute(player)
        val metadataMap = getMetadataMap(player)
        return PlayerProfile(getUserId(player).id, player, route, metadataMap)
    }

    private fun getMetadataMap(player: Player): Map<String, Metadata> {
        val userId = getUserId(player).id
        return tableMetadata.select(dataSource) {
            where { "user" eq userId }
            rows("node", "type", "token", "stop_time")
        }.map {
            val type = Class.forName(getString("type"))
            val token = getString("token")
            val stopTime = getTimestamp("stop_time").time
            getString("node") to Metadata.Loader.parseTypeToken(type, token, stopTime)
        }.toMap()
    }

    private fun getUserId(player: Player): Id {
        if (this.cachedId.containsKey(player.uniqueId)) {
            return Id(this.cachedId[player.uniqueId]!!, false)
        }
        // 添加到map 由于id是唯一的 不用在join和quit维护id
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
        }
        // 新建用户id
        else {
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
        // 如果id是新的 则不查询后续操作
        if (userId.created) {
            return null
        }

        return tableUser.select(dataSource) {
            where { "id" eq getUserId(player) }
            rows("route")
        }.first { getRouteById(getLong("route")) }
    }

    private fun getPlayerSkills(route: Long): List<PlayerSkill> {
        return tableSkill.select(dataSource) {
            where { "route" eq route }
            rows("id", "node", "level")
        }.map { PlayerSkill(getLong("id"), getString("node"), getInt("level")) }
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

    override fun updateMetadata(profile: PlayerProfile, id: String, metadata: Metadata) {

        // 虚空节点 || 节点超时 删除
        if (metadata is MetadataTypeToken.Void || metadata.isTimeout()) {
            tableMetadata.delete(dataSource) { whereWithMetadata(profile, id) }
        }
        // 更新节点
        else if (tableMetadata.find(dataSource) { whereWithMetadata(profile, id) }) {
            tableMetadata.update(dataSource) {
                whereWithMetadata(profile, id)
                set("type", metadata.clazz.name)
                set("token", Metadata.Loader.toJson(metadata))
                set("stop_time", metadata.stopTime)
            }
        }
        // 插入节点
        else {
            tableMetadata.insert(dataSource, "type", "token", "stop_time") {
                value(metadata.clazz.name, Metadata.Loader.toJson(metadata), metadata.stopTime)
            }
        }
    }

    fun ActionFilterable.whereWithMetadata(profile: PlayerProfile, id: String) {
        return where {
            "user" eq profile.id
            "node" eq id
        }
    }

    override fun createPlayerSkill(profile: PlayerProfile, skill: ImmutableSkill): CompletableFuture<PlayerSkill> {
        val future = CompletableFuture<PlayerSkill>()
        tableSkill.insert(dataSource, "route", "node", "level") {
            value(
                profile.route?.id ?: error("Player ${profile.onlinePlayer.name} not find route"),
                skill.id,
                skill.startedLevel
            )
            onFinally {
                val id = getId(generatedKeys)
                future.complete(PlayerSkill(id, skill.id, skill.startedLevel))
            }
        }
        return future
    }

    override fun createPlayerJob(profile: PlayerProfile, route: ImmutableRoute): CompletableFuture<PlayerRoute> {
        val future = CompletableFuture<PlayerRoute>()
        val node = PlayerRoute.Node(profile.route?.bindingId ?: -1L, route.id)
        tableRoute.insert(dataSource, "router", "parent", "route") {
            value(route.routerId, node.parentId, node.route)
            onFinally {
                future.complete(PlayerRoute(getId(generatedKeys), route.routerId, node, emptyList()))
            }
        }
        return future
    }

    private fun getId(resultSet: ResultSet): Long {
        resultSet.next()
        return resultSet.getLong(1)
    }

    class Id(val id: Long, val created: Boolean)

}
