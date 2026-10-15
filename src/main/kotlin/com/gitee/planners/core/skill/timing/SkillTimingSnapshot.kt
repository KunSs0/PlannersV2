package com.gitee.planners.core.skill.timing

/**
 * 一次技能释放的最终时间快照，不允许用最终时间再次计算急速。
 *
 * @property cooldownTicks 最终冷却tick；未声明冷却变量时为null。
 * @property castTimeTicks 最终施法等待tick，零表示配置明确要求立即执行。
 */
data class SkillTimingSnapshot(val cooldownTicks: Int?, val castTimeTicks: Int)
