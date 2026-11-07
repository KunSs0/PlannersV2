package com.gitee.planners.module.action

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.api.event.player.PlayerExperienceEvent
import com.gitee.planners.api.event.player.PlayerLevelChangedEvent
import com.gitee.planners.api.event.player.PlayerMagicPointEvent
import com.gitee.planners.api.event.player.PlayerRouteClearedEvent
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.event.player.PlayerSkillCooldownEvent
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.api.event.player.PlayerSkillTreeNodeEvent
import com.gitee.planners.api.event.player.TargetCapturedEvent
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.skill.context.SkillExecutionContext
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning

/**
 * Planners 事件到 Action 触发器的桥接。
 *
 * 每个事件只做三件事：取出触发玩家、装配执行上下文、按触发器主键派发。
 * 是否真正执行由技能自己的流程条件决定，桥接层不做等级或职业判断。
 *
 * 单个事件的派发异常只记录，不影响事件其他监听器。
 */
object ActionEventBridge {

    /** 技能进入施法流程，取 Pre 阶段，每次施法只派发一次。 */
    @SubscribeEvent
    fun onSkillCast(event: PlayerSkillCastEvent.Pre) {
        val skill = event.skill
        val level = resolveLevel(event.player, skill.id)
        val scope = ActionScopeFactory.forPlayer(
            event.player,
            skill.immutable,
            level,
            emptyMap(),
            null,
            "skill:" + skill.id,
        )
        dispatch(ActionTriggers.SkillCast.id, scope)
    }

    /** 技能冷却被设置。 */
    @SubscribeEvent
    fun onSkillCooldown(event: PlayerSkillCooldownEvent.Set) {
        val scope = ActionScopeFactory.forPlayer(
            event.player,
            null,
            0,
            mapOf("cooldownTicks" to event.ticks, "skillId" to event.skill.id),
            null,
            "skill-cooldown",
        )
        dispatch(ActionTriggers.SkillCooldown.id, scope)
    }

    /** 玩家首次学会技能。 */
    @SubscribeEvent
    fun onSkillLearn(event: PlayerSkillEvent.Learn) {
        val player = event.player
        val skill = event.skill
        val scope = ActionScopeFactory.forPlayer(
            player,
            skill.immutable,
            1,
            emptyMap(),
            null,
            "skill:" + skill.id,
        )
        dispatch(ActionTriggers.SkillLearn.id, scope)
    }

    /** 技能等级变化。 */
    @SubscribeEvent
    fun onSkillLevel(event: PlayerSkillEvent.LevelChange) {
        val player = event.player
        val skill = event.skill
        val scope = ActionScopeFactory.forPlayer(
            player,
            skill.immutable,
            event.to,
            mapOf("from" to event.form, "to" to event.to),
            null,
            "skill:" + skill.id,
        )
        dispatch(ActionTriggers.SkillLevel.id, scope)
    }

    /** 技能树节点变化。 */
    @SubscribeEvent
    fun onSkillNode(event: PlayerSkillTreeNodeEvent) {
        val scope = ActionScopeFactory.forPlayer(
            event.player,
            null,
            0,
            mapOf("tree" to event.tree.id, "node" to event.node.id, "from" to event.from, "to" to event.to),
            null,
            "skilltree:" + event.tree.id,
        )
        dispatch(ActionTriggers.SkillNode.id, scope)
    }

    /** 角色等级变化。 */
    @SubscribeEvent
    fun onLevel(event: PlayerLevelChangedEvent) {
        val scope = ActionScopeFactory.forPlayer(
            event.player,
            null,
            0,
            mapOf("from" to event.from, "to" to event.to),
            null,
            "level",
        )
        dispatch(ActionTriggers.Level.id, scope)
    }

    /** 经验变化。 */
    @SubscribeEvent
    fun onExperience(event: PlayerExperienceEvent) {
        val scope = ActionScopeFactory.forPlayer(event.player, null, 0, emptyMap(), null, "experience")
        dispatch(ActionTriggers.Experience.id, scope)
    }

    /** 法力值变化。 */
    @SubscribeEvent
    fun onMagicPoint(event: PlayerMagicPointEvent) {
        val scope = ActionScopeFactory.forPlayer(event.player, null, 0, emptyMap(), null, "magic-point")
        dispatch(ActionTriggers.MagicPoint.id, scope)
    }

    /** 目标被捕获。 */
    @SubscribeEvent
    fun onTargetCaptured(event: TargetCapturedEvent) {
        val sender = event.sender.instance
        if (sender !is Player) {
            return
        }
        val scope = ActionScopeFactory.forPlayer(
            sender,
            null,
            0,
            mapOf("cause" to event.cause.name, "targetCount" to event.container.size),
            null,
            "target-captured",
        )
        dispatch(ActionTriggers.TargetCaptured.id, scope)
    }

    /** 玩家对实体造成伤害。 */
    @SubscribeEvent
    fun onPlayerDamageEntity(event: PlayerDamageEntityEvent) {
        val scope = ActionScopeFactory.forPlayer(
            event.player,
            null,
            0,
            mapOf("damage" to event.damage, "cause" to event.cause.name),
            event.entity,
            "player-damage-entity",
        )
        dispatch(ActionTriggers.PlayerDamageEntity.id, scope)
    }

    /** 职业被清空。 */
    @SubscribeEvent
    fun onRouteCleared(event: PlayerRouteClearedEvent) {
        val scope = ActionScopeFactory.forPlayer(
            event.player,
            null,
            0,
            mapOf("previousRouterId" to event.previousRouterId),
            null,
            "route-cleared",
        )
        dispatch(ActionTriggers.RouteCleared.id, scope)
    }

    /** 玩家退出。 */
    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) {
        val scope = ActionScopeFactory.forPlayer(event.player, null, 0, emptyMap(), null, "quit")
        dispatch(ActionTriggers.Quit.id, scope)
    }

    /**
     * 派发一次触发器，异常只记录不抛出。
     *
     * @param triggerId 触发器主键。
     * @param scope 本次执行上下文。
     */
    private fun dispatch(triggerId: String, scope: com.gitee.planners.module.action.query.QueryScope) {
        if (!ActionTriggerRegistry.hasTrigger(triggerId)) {
            return
        }
        try {
            ActionTriggerRegistry.dispatch(triggerId) { scope }
        } catch (exception: Exception) {
            warning("Action trigger '$triggerId' failed: ${exception.message}")
        }
    }

    /**
     * 解析玩家当前技能等级。
     *
     * @param player 玩家。
     * @param skillId 技能 ID。
     * @return 技能等级；未穿戴或档案未加载时返回 0。
     */
    private fun resolveLevel(player: Player, skillId: String): Int {
        return try {
            val template = player.plannersTemplate
            com.gitee.planners.core.skilltree.SkillTreeNodeEffectService.getSkillLevel(template, skillId)
        } catch (exception: IllegalStateException) {
            0
        }
    }
}
