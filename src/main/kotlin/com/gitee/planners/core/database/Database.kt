package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.event.DatabaseInitEvent
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import com.gitee.planners.util.configNodeTo
import org.bukkit.entity.Player
import taboolib.module.configuration.ConfigNode
import java.util.concurrent.CompletableFuture

interface Database {

    companion object {

        @ConfigNode("database")
        val option = configNodeTo { DatabaseOption(this) }

        val INSTANCE: Database by lazy {
            when (val type = option.get().use.uppercase()) {
                "LOCAL" -> DatabaseLocal()
                "SQL" -> DatabaseSQL()
                else -> {
                    val event = DatabaseInitEvent(type)
                    event.call()
                    event.instance ?: error("Unsupported database type: $type")
                }
            }
        }

    }

    /**
     * 加载玩家完整档案
     * 包含当前 route、metadata、已学技能等全部数据
     */
    fun getPlayerProfile(player: Player): PlayerTemplate

    /** 写入或删除一条 metadata */
    fun updateMetadata(template: PlayerTemplate, id: String, metadata: Metadata)

    /** 批量删除技能记录 */
    fun deleteSkill(vararg skill: PlayerSkill)

    /** 更新技能等级、装备状态、背包位置 */
    fun updateSkill(skill: PlayerSkill)

    /**
     * 同步提交学习成本、节点及初次技能记录，成功后立即回调发布内存状态。
     * 外部资源只由 LearningCosts 处理；存储不依赖任何道具或经济插件。
     *
     * @param router 被学习锁保护的职业线。
     * @param route 目标职业阶段。
     * @param treeId 技能树标识。
     * @param nodeId 真实节点标识。
     * @param level 提交前等级。
     * @param costs 已准备成本。
     * @param skill 需要初次创建的技能定义，不需要时为 null。
     * @param onCommitted 数据库成功提交后立即发布状态；回调失败不得退款。
     */
    fun purchaseSkillTreeNode(
        router: PlayerRouter,
        route: PlayerRoute,
        treeId: String,
        nodeId: String,
        level: Int,
        costs: com.gitee.planners.core.condition.LearningCosts,
        skill: ImmutableSkill?,
        onCommitted: (SkillTreePurchaseStore.Result, Int) -> Unit
    )

    /** 为已激活的技能节点创建运行时技能记录。 */
    fun createPlayerSkill(template: PlayerTemplate, route: PlayerRoute, skill: ImmutableSkill): CompletableFuture<PlayerSkill>

    /**
     * 创建子路线（转职）
     * @param parentId 父路线在数据库中的 id
     */
    fun createPlayerRoute(router: PlayerRouter, parentId: Long, route: ImmutableRoute): CompletableFuture<PlayerRoute>

    /**
     * 查询玩家在指定 router 下的等级数据
     * @return 未找到返回 null
     */
    fun loadPlayerRouter(userId: Long, routerId: String): PlayerRouter?

    /** 为玩家在指定 router 下创建初始等级记录 */
    fun createPlayerRouter(userId: Long, routerId: String, initialLevel: Int): PlayerRouter

    /** 更新玩家在 router 下的等级和经验值 */
    fun updatePlayerRouter(router: PlayerRouter)

    /** 删除玩家当前选择的 Router 及其转职线数据。 */
    fun deletePlayerRouter(router: PlayerRouter)

}
