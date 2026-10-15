package com.gitee.planners.api.timing

import org.bukkit.entity.Player

/**
 * 外部属性系统提供的技能时间修正入口；Planners不重复实现属性换算公式。
 */
fun interface SkillTimingProvider {
    /**
     * 在释放发起线程捕获一次属性收益，两项时间共用该快照。
     *
     * @param player 本次技能施放者。
     * @return 明确的冷却和施法缩短比例。
     * @throws IllegalArgumentException 属性或关联配置非法时抛出。
     */
    fun capture(player: Player): SkillTimingModifiers
}
