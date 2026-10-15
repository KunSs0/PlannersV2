package com.gitee.planners.api.timing

/**
 * 一次释放使用的技能时间缩短比例快照。
 *
 * @property cooldownReduction 冷却缩短比例，范围为0～1。
 * @property castTimeReduction 施法缩短比例，范围为0～1。
 * @throws IllegalArgumentException 任一比例不是有限数或越界时抛出。
 */
data class SkillTimingModifiers(val cooldownReduction: Double, val castTimeReduction: Double) {
    init {
        // 在跨插件边界校验单位与范围，不能用限幅隐藏错误实现。
        require(cooldownReduction.isFinite() && cooldownReduction in 0.0..1.0) { "技能冷却缩短比例必须为0～1的有限数。" }
        require(castTimeReduction.isFinite() && castTimeReduction in 0.0..1.0) { "技能施法缩短比例必须为0～1的有限数。" }
    }
}
