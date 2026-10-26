package com.gitee.planners.core.database

import java.sql.Connection
import java.sql.Statement
import javax.sql.DataSource

/**
 * LOCAL/SQL 共用的阻塞 JDBC 写入器，只由后台串行队列调用。
 *
 * 本类只消费字段快照，不派发 Bukkit 事件、不扣外部资源、不读写在线职业对象。
 * @param dataSource 已初始化的数据源。
 * @param prefix 已验证的表名前缀。
 */
class ProfileStore(private val dataSource: DataSource, private val prefix: String) {
    init {
        require(prefix.matches(Regex("[A-Za-z0-9_]+"))) { "数据库表名前缀非法" }
    }

    /**
     * 将新阶段和职业线当前指针共同提交，失败不留下半条新职业线。
     * @param router 已发布的职业线状态。
     * @param route 已发布的新阶段。
     * @param initial 是否同时插入新职业线。
     */
    fun createRoute(router: ProfileSnapshot.Router, route: ProfileSnapshot.Route, initial: Boolean) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val routerId: Long
                if (initial) {
                    routerId = insert(connection, "router", "user, router, level, experience, current_route, sp_current, sp_used",
                        listOf(router.user, router.router, router.level, router.experience, -1L, router.points, router.used))
                } else {
                    routerId = router.identity.persisted()
                }
                val routeId = insert(connection, "route", "user, router, parent, route",
                    listOf(route.user, route.router, route.parent?.persisted() ?: -1L, route.job))
                execute(connection, "UPDATE `${prefix}_router` SET current_route=? WHERE id=?", listOf(routeId, routerId))
                connection.commit()
                // 生成主键只保存在后台身份句柄，在线集合的 key 从不改变。
                if (initial) {
                    router.identity.inserted(routerId)
                }
                route.identity.inserted(routeId)
            } catch (failure: Throwable) {
                try {
                    connection.rollback()
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
        }
    }

    /**
     * 保存一份最终职业线状态，不重新读取可变内存。
     * @param snapshot 提交时的等级、经验、当前职业和技能点。
     */
    fun saveRouter(snapshot: ProfileSnapshot.Router) {
        val current = snapshot.currentRoute?.persisted() ?: -1L
        execute("UPDATE `${prefix}_router` SET level=?, experience=?, current_route=?, sp_current=?, sp_used=? WHERE id=?",
            listOf(snapshot.level, snapshot.experience, current, snapshot.points, snapshot.used, snapshot.identity.persisted()))
    }

    /**
     * 创建技能记录，稳定内存身份不因生成主键改变。
     * @param route 所属职业阶段身份。
     * @param snapshot 已发布技能字段。
     */
    fun createSkill(route: RecordIdentity, snapshot: ProfileSnapshot.Skill) {
        snapshot.identity.inserted(insert("skill", "route, node, level, equipped, backpack_page, backpack_slot",
            listOf(route.persisted(), snapshot.skill, snapshot.level, if (snapshot.equipped) 1 else 0, snapshot.page, snapshot.slot)))
    }

    /**
     * 保存技能等级与装备字段；队列顺序阻止旧保存覆盖新操作。
     * @param snapshot 提交时的技能状态。
     */
    fun saveSkill(snapshot: ProfileSnapshot.Skill) {
        execute("UPDATE `${prefix}_skill` SET level=?, equipped=?, backpack_page=?, backpack_slot=? WHERE id=?",
            listOf(snapshot.level, if (snapshot.equipped) 1 else 0, snapshot.page, snapshot.slot, snapshot.identity.persisted()))
    }

    /**
     * 将已完成内存学习的技能点、节点和可选新技能作为一个后台事务保存。
     *
     * JDBC 失败只回滚数据库事务；绝不退款或回滚已经完成的在线业务。
     * @param router 学习后的职业线快照。
     * @param route 节点所属阶段。
     * @param node 学习后的节点。
     * @param skill 本次首次创建的技能，没有时为 null。
     */
    fun saveLearning(router: ProfileSnapshot.Router, route: RecordIdentity, node: ProfileSnapshot.Node, skill: ProfileSnapshot.Skill?) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val nodeId: Long
                if (node.identity.runtimeId < -1 && node.level == 1) {
                    nodeId = insert(connection, "skill_tree_node", "route, tree, node, level",
                        listOf(route.persisted(), node.tree, node.node, node.level))
                } else {
                    nodeId = node.identity.persisted()
                    execute(connection, "UPDATE `${prefix}_skill_tree_node` SET level=? WHERE id=?", listOf(node.level, nodeId))
                }
                var skillId: Long? = null
                if (skill != null) {
                    skillId = insert(connection, "skill", "route, node, level, equipped, backpack_page, backpack_slot",
                        listOf(route.persisted(), skill.skill, skill.level, if (skill.equipped) 1 else 0, skill.page, skill.slot))
                }
                execute(connection, "UPDATE `${prefix}_router` SET level=?, experience=?, current_route=?, sp_current=?, sp_used=? WHERE id=?",
                    listOf(router.level, router.experience, router.currentRoute?.persisted() ?: -1L, router.points, router.used, router.identity.persisted()))
                connection.commit()
                // 只有提交完成后才向后继 SQL 暴露数据库身份。
                if (node.identity.runtimeId < -1 && node.level == 1) {
                    node.identity.inserted(nodeId)
                }
                if (skill != null && skillId != null) {
                    skill.identity.inserted(skillId)
                }
            } catch (failure: Throwable) {
                try {
                    connection.rollback()
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
        }
    }

    /**
     * 按旧记录身份清空职业；与前序插入、学习、装备保存按提交顺序执行。
     * @param snapshot 主线程清空时捕获的旧身份集合。
     */
    fun deleteRouter(snapshot: ProfileSnapshot.Deletion) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                for (route in snapshot.routes) {
                    val id = route.persisted()
                    execute(connection, "DELETE FROM `${prefix}_skill` WHERE route=?", listOf(id))
                    execute(connection, "DELETE FROM `${prefix}_skill_tree_node` WHERE route=?", listOf(id))
                    execute(connection, "DELETE FROM `${prefix}_route` WHERE id=?", listOf(id))
                }
                execute(connection, "DELETE FROM `${prefix}_router` WHERE id=?", listOf(snapshot.router.persisted()))
                connection.commit()
            } catch (failure: Throwable) {
                try {
                    connection.rollback()
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
        }
    }

    /**
     * 保存已序列化元数据，删除与新增均在后台执行。
     * @param snapshot 当前用户和字段的新值，type 为 null 表示删除。
     */
    fun saveMetadata(snapshot: ProfileSnapshot.Metadata) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                execute(connection, "DELETE FROM `${prefix}_metadata` WHERE user=? AND node=?", listOf(snapshot.user, snapshot.key))
                if (snapshot.type != null) {
                    insert(connection, "metadata", "user, node, type, token, stop_time",
                        listOf(snapshot.user, snapshot.key, snapshot.type, snapshot.token, snapshot.expires))
                }
                connection.commit()
            } catch (failure: Throwable) {
                try {
                    connection.rollback()
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
        }
    }

    /** 打开独立连接执行单条字段更新，所有值使用参数绑定。 */
    private fun execute(sql: String, values: List<Any?>) {
        dataSource.connection.use { connection ->
            execute(connection, sql, values)
        }
    }

    /** 使用调用方事务连接执行更新，不触碰在线对象。 */
    private fun execute(connection: Connection, sql: String, values: List<Any?>) {
        connection.prepareStatement(sql).use { statement ->
            for (index in values.indices) {
                statement.setObject(index + 1, values[index])
            }
            statement.executeUpdate()
        }
    }

    /** 打开独立连接插入一条记录，读取数据库生成的主键。 */
    private fun insert(suffix: String, columns: String, values: List<Any?>): Long {
        dataSource.connection.use { connection ->
            return insert(connection, suffix, columns, values)
        }
    }

    /** 在指定后台事务内插入一条记录，列名来自固定源码。 */
    private fun insert(connection: Connection, suffix: String, columns: String, values: List<Any?>): Long {
        val placeholders = ArrayList<String>()
        for (ignored in values) {
            placeholders.add("?")
        }
        connection.prepareStatement("INSERT INTO `${prefix}_$suffix` ($columns) VALUES (${placeholders.joinToString(",")})", Statement.RETURN_GENERATED_KEYS).use { statement ->
            for (index in values.indices) {
                statement.setObject(index + 1, values[index])
            }
            check(statement.executeUpdate() == 1) { "记录创建失败: $suffix" }
            statement.generatedKeys.use { rows ->
                check(rows.next()) { "未生成记录主键: $suffix" }
                return rows.getLong(1)
            }
        }
    }
}
