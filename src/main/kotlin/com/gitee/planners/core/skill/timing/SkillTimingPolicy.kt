package com.gitee.planners.core.skill.timing

import taboolib.library.configuration.ConfigurationSection
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/**
 * 从显式配置解析的技能时间策略。
 *
 * @property provider 属性实现ID；unmodified表示明确选择不应用属性修正。
 * @property rounding tick取整策略。
 */
class SkillTimingPolicy private constructor(val provider: String, private val rounding: String) {
    /**
     * 将公式得到的时间按配置转换为调度器tick数。
     *
     * @param ticks 尚未取整的时间，单位tick。
     * @return 可用于冷却和调度的非负整数tick数。
     * @throws IllegalArgumentException 时间非法或超出整数范围时抛出。
     */
    fun toTicks(ticks: Double): Int {
        // 先校验再按显式策略取整，禁止toInt饱和截断隐藏溢出。
        require(ticks.isFinite() && ticks >= 0.0) { "技能时间必须为非负有限tick数。" }
        val rounded = when (rounding) {
            "ceil" -> {
                ceil(ticks)
            }
            "floor" -> {
                floor(ticks)
            }
            "nearest" -> {
                round(ticks)
            }
            else -> {
                error("非法的技能时间取整策略：$rounding")
            }
        }
        require(rounded <= Int.MAX_VALUE.toDouble()) { "技能时间超过整数tick范围：$ticks" }
        return rounded.toInt()
    }

    companion object {
        /**
         * 解析配置中的Provider与tick取整策略，所有字段均为必填。
         *
         * @param config settings.skill.timing配置节。
         * @return 经过校验的时间策略。
         * @throws IllegalArgumentException 缺项、类型错误或未知取整策略时抛出。
         */
        fun parse(config: ConfigurationSection?): SkillTimingPolicy {
            // 不使用getString默认值，缺项必须报告实际配置路径。
            require(config != null) { "缺少settings.skill.timing配置节。" }
            val provider = config["provider"]
            require(provider is String && provider.isNotBlank()) { "settings.skill.timing.provider必须为非空字符串。" }
            val rounding = config["tick-rounding"]
            require(rounding is String && rounding in listOf("ceil", "floor", "nearest")) {
                "settings.skill.timing.tick-rounding必须显式配置为ceil、floor或nearest。"
            }
            return SkillTimingPolicy(provider, rounding)
        }
    }
}
