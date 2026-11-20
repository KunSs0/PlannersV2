package com.gitee.planners.api.pick

/**
 * `entity` 指向定义：以单个实体为结果。
 *
 * @property maxDurationTicks 会话最长保持 tick 数。
 * @property range 候选实体最大距离。
 * @property maxAngleDegrees 候选实体与瞄准方向允许的最大夹角。
 * @property requireLineOfSight 是否要求施法者与目标之间视线无遮挡。
 * @property fallback 未选中任何合规实体时的兜底策略；null 表示直接按失败结算。
 */
class EntityPickDefinition(
    override val maxDurationTicks: Int,
    val range: Double,
    val maxAngleDegrees: Double,
    val requireLineOfSight: Boolean,
    val fallback: String?,
) : PickDefinition {

    override val type = TYPE

    override fun fields(): Map<String, Any?> {
        return mapOf(
            "range" to range,
            "maxAngleDegrees" to maxAngleDegrees,
            "requireLineOfSight" to requireLineOfSight,
            "fallback" to fallback,
        )
    }

    companion object {

        /** 类型标识。 */
        const val TYPE = "entity"

        /** 未选中合规实体时改为以施法者本人作为本次指向结果。 */
        const val FALLBACK_SELF = "self"
    }
}
