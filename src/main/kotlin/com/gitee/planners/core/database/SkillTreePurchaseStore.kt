package com.gitee.planners.core.database

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import java.sql.Connection
import java.sql.Statement

/**
 * JDBC 学习事务；LOCAL 和 SQL 共用相同参数化语句，不接触外部插件。
 * @param prefix 经过配置解析的表名前缀。
 */
class SkillTreePurchaseStore(private val prefix: String) {
    init {
        require(prefix.matches(Regex("[A-Za-z0-9_]+"))) { "学习事务表名前缀非法" }
    }

    /** 已提交的节点与可选初次技能记录，提交前不得发布到玩家内存。 */
    class Result(val state: PlayerSkillTreeNodeState, val skill: PlayerSkill?)

    /**
     * 在同一连接中扣除技能点、写入节点并创建需要的技能记录，最后提交外部资源。
     * 调用方必须持有玩家学习锁，在主线程同步调用并在失败时回滚外部资源。
     *
     * @param connection 本次独占的连接，调用方负责关闭。
     * @param routerId 职业线数据库主键。
     * @param routeId 职业阶段数据库主键。
     * @param treeId 技能树标识。
     * @param nodeId 真实节点标识。
     * @param currentLevel 请求开始时的节点等级。
     * @param currentPoints 当前内存可用技能点，用于并发一致性检查。
     * @param usedPoints 当前内存已用技能点。
     * @param amount 本次技能点成本。
     * @param newSkillId 首次激活技能时需要创建的技能 ID。
     * @param newSkillLevel 新技能的初始等级。
     * @param commitCosts 主线程外部资源提交入口。
     * @return 数据库提交成功后的新状态。
     */
    fun purchase(
        connection: Connection,
        routerId: Long,
        routeId: Long,
        treeId: String,
        nodeId: String,
        currentLevel: Int,
        currentPoints: Int,
        usedPoints: Int,
        amount: Int,
        newSkillId: String?,
        newSkillLevel: Int,
        commitCosts: () -> Unit
    ): Result {
        require(amount >= 0 && currentPoints >= amount) { "技能点不足或成本非法" }
        val nextUsed = Math.addExact(usedPoints, amount)
        check(connection.autoCommit) { "学习事务需要独立连接" }
        connection.autoCommit = false
        try {
            // CAS 同时检查当前职业线的两项技能点，拒绝陈旧档案覆盖数据库。
            connection.prepareStatement("SELECT sp_current, sp_used FROM `${prefix}_router` WHERE id = ?").use { statement ->
                statement.setLong(1, routerId)
                statement.executeQuery().use { rows ->
                    check(rows.next() && rows.getInt(1) == currentPoints && rows.getInt(2) == usedPoints) { "技能点状态已经改变，请重新加载档案" }
                }
            }
            if (amount > 0) {
                connection.prepareStatement("UPDATE `${prefix}_router` SET sp_current = ?, sp_used = ? WHERE id = ? AND sp_current = ? AND sp_used = ?").use { statement ->
                    statement.setInt(1, currentPoints - amount)
                    statement.setInt(2, nextUsed)
                    statement.setLong(3, routerId)
                    statement.setInt(4, currentPoints)
                    statement.setInt(5, usedPoints)
                    check(statement.executeUpdate() == 1) { "技能点状态已经改变" }
                }
            }
            var index: Long? = null
            connection.prepareStatement("SELECT id, level FROM `${prefix}_skill_tree_node` WHERE route = ? AND tree = ? AND node = ?").use { statement ->
                statement.setLong(1, routeId)
                statement.setString(2, treeId)
                statement.setString(3, nodeId)
                statement.executeQuery().use { rows ->
                    if (rows.next()) {
                        check(rows.getInt(2) == currentLevel) { "节点等级已经改变" }
                        index = rows.getLong(1)
                        check(!rows.next()) { "节点存在重复记录" }
                    } else {
                        check(currentLevel == 0) { "节点记录缺失" }
                    }
                }
            }
            val previousIndex = index
            if (previousIndex == null) {
                index = insert(connection, "skill_tree_node", "route, tree, node, level", listOf(routeId, treeId, nodeId, currentLevel + 1))
            } else {
                connection.prepareStatement("UPDATE `${prefix}_skill_tree_node` SET level = ? WHERE id = ? AND level = ?").use { statement ->
                    statement.setInt(1, currentLevel + 1)
                    statement.setLong(2, previousIndex)
                    statement.setInt(3, currentLevel)
                    check(statement.executeUpdate() == 1) { "节点等级已经改变" }
                }
            }
            var skill: PlayerSkill? = null
            if (newSkillId != null) {
                val skillIndex = insert(connection, "skill", "route, node, level", listOf(routeId, newSkillId, newSkillLevel))
                skill = PlayerSkill(skillIndex, newSkillId, newSkillLevel)
            }
            val result = Result(PlayerSkillTreeNodeState(index, treeId, nodeId, currentLevel + 1), skill)
            // 直到所有 SQL 成功后才扣外部资源；期间不让出主线程，背包不能被玩家操作。
            commitCosts()
            connection.commit()
            return result
        } catch (failure: Throwable) {
            try {
                connection.rollback()
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }
            throw failure
        }
    }

    /**
     * 插入固定结构的节点或技能记录并读取生成主键，所有业务值使用参数绑定。
     * @param connection 当前事务连接。
     * @param suffix 内部固定表后缀。
     * @param columns 内部固定列声明。
     * @param values 列值。
     * @return 新记录主键。
     */
    private fun insert(connection: Connection, suffix: String, columns: String, values: List<Any>): Long {
        val placeholders = ArrayList<String>()
        for (ignored in values) {
            placeholders.add("?")
        }
        connection.prepareStatement("INSERT INTO `${prefix}_$suffix` ($columns) VALUES (${placeholders.joinToString(",")})", Statement.RETURN_GENERATED_KEYS).use { statement ->
            for (index in values.indices) {
                statement.setObject(index + 1, values[index])
            }
            check(statement.executeUpdate() == 1) { "学习记录创建失败" }
            statement.generatedKeys.use { rows ->
                check(rows.next()) { "学习记录缺少生成主键" }
                return rows.getLong(1)
            }
        }
    }
}
