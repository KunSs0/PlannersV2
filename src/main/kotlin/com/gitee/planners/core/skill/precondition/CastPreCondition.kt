package com.gitee.planners.core.skill.precondition

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.module.script.ScriptOptions
import org.bukkit.entity.Player

/**
 * 技能释放前条件。
 *
 * 通过 [com.gitee.planners.api.PlannersAPI.registerCastPreCondition] 注册。
 * 校验流程按 [priority] 升序执行：verify → consume。
 */
interface CastPreCondition {

    /** 条件标识，如 "cooldown"、"mp"、"stamina" */
    val key: String

    /** 执行顺序，越小越先执行 */
    val priority: Int

    /**
     * 条件不满足时的提示消息。
     *
     * @param player 触发失败的玩家
     * @param failure 失败结果，携带上下文数据
     * @return 提示消息，支持 {key} 占位符（由调用方替换）
     */
    fun hint(player: Player, failure: CastPreConditionResult.Failure): String

    /**
     * 校验条件。
     *
     * @return 校验结果，返回 null 表示该技能不涉及此资源（跳过）
     */
    fun verify(player: Player, skill: PlayerSkill, options: ScriptOptions): CastPreConditionResult?

    /**
     * 校验通过后消耗资源。
     *
     * 默认无操作，需要消耗资源的条件重写此方法。
     */
    fun consume(player: Player, skill: PlayerSkill, options: ScriptOptions) {}

}
