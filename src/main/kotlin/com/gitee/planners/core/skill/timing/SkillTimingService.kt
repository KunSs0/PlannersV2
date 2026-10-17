package com.gitee.planners.core.skill.timing

import com.gitee.planners.Planners
import com.gitee.planners.api.timing.SkillTimingModifiers
import com.gitee.planners.api.timing.SkillTimingProviders
import com.gitee.planners.core.skill.context.SkillExecutionContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * 在技能释放开始时捕获属性并计算一次冷却和施法时间。
 */
object SkillTimingService {
    /**
     * 按当前显式配置准备技能时间，只能在Bukkit主线程读取玩家属性。
     *
     * @param player 本次施放者。
     * @param execution 已完成基础变量计算的执行上下文。
     * @return 同一执行上下文唯一的最终时间快照。
     * @throws IllegalArgumentException 配置或基础时间非法时抛出。
     * @throws IllegalStateException 线程错误或Provider缺失时抛出。
     */
    fun prepare(player: Player, execution: SkillExecutionContext): SkillTimingSnapshot {
        // 外部属性容器可能读取实体状态，必须在技能发起线程完成快照。
        check(Bukkit.isPrimaryThread()) { "技能时间必须在Bukkit主线程准备。" }
        // 策略由配置节点统一重载，技能热路径不再遍历配置树或重复解析。
        return prepare(player, execution, Planners.skillTimingPolicy.get())
    }

    /**
     * 使用显式策略准备一次最终时间，便于宿主和合成测试使用相同生产路径。
     *
     * @param player 本次施放者。
     * @param execution 已完成基础变量计算的执行上下文。
     * @param policy 已校验的显式时间策略。
     * @return 已存在或新生成的唯一时间快照。
     * @throws IllegalArgumentException 基础时间非法时抛出。
     * @throws IllegalStateException 指定Provider不存在时抛出。
     */
    fun prepare(player: Player, execution: SkillExecutionContext, policy: SkillTimingPolicy): SkillTimingSnapshot {
        // 先读取缓存，指向确认、最终校验及Hook恢复都不能二次缩短时间。
        val previous = execution.timing
        if (previous != null) {
            return previous
        }
        val castTime = requireTime(execution.getVariable("castTime"), "技能${execution.skill.id}.__option__.variables.castTime")
        val rawCooldown = execution.getVariable("cooldown")
        val cooldown: Double?
        if (!execution.variableSnapshot().containsKey("cooldown")) {
            cooldown = null
        } else {
            cooldown = requireTime(rawCooldown, "技能${execution.skill.id}.__option__.variables.cooldown")
        }
        val modifiers: SkillTimingModifiers
        if (policy.provider == "unmodified") {
            // 仅在配置明确选择原时间模式时不应用属性，不是Provider缺失兜底。
            modifiers = SkillTimingModifiers(0.0, 0.0)
        } else {
            modifiers = SkillTimingProviders.requireProvider(policy.provider).capture(player)
        }
        val cooldownTicks: Int?
        if (cooldown == null) {
            cooldownTicks = null
        } else {
            cooldownTicks = policy.toTicks(cooldown * (1.0 - modifiers.cooldownReduction))
        }
        val castingTicks = policy.toTicks(castTime * (1.0 - modifiers.castTimeReduction))
        val snapshot = SkillTimingSnapshot(cooldownTicks, castingTicks)
        execution.timing = snapshot
        execution.setVariable("castTime", castingTicks)
        if (cooldownTicks != null) {
            execution.setVariable("cooldown", cooldownTicks)
        }
        return snapshot
    }

    /**
     * 校验基础变量的类型、单位和范围，不把缺失或字符串强转成零。
     *
     * @param value 基础时间变量原值。
     * @param path 可直接定位的字段路径。
     * @return 非负有限tick数。
     * @throws IllegalArgumentException 缺项或非法数值时抛出。
     */
    private fun requireTime(value: Any?, path: String): Double {
        // Nova表达式可以返回整数或浮点数，但不接受文本和布尔值。
        require(value is Number) { "${path}必须显式提供非负有限tick数。" }
        val ticks = value.toDouble()
        require(ticks.isFinite() && ticks >= 0.0 && ticks <= Int.MAX_VALUE.toDouble()) { "${path}超出合法tick范围。" }
        return ticks
    }
}
