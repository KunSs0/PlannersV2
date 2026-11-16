package com.gitee.planners.api.pick

/**
 * `entity` 指向定义：以单个实体为结果。
 *
 * @property maxDurationTicks 会话最长保持 tick 数。
 * @property range 候选实体最大距离。
 * @property maxAngleDegrees 候选实体与瞄准方向允许的最大夹角。
 * @property requireLineOfSight 是否要求施法者与目标之间视线无遮挡。
 */
class EntityPickDefinition(
    override val maxDurationTicks: Int,
    val range: Double,
    val maxAngleDegrees: Double,
    val requireLineOfSight: Boolean,
) : PickDefinition {

    override val type = TYPE

    companion object {

        /** 类型标识。 */
        const val TYPE = "entity"
    }
}
