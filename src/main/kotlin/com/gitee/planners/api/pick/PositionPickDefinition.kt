package com.gitee.planners.api.pick

/**
 * `position` 指向定义：以世界上一个可站立的位置为结果。
 *
 * @property maxDurationTicks 会话最长保持 tick 数。
 * @property range 落点最大距离，以施法者眼睛为起点。
 * @property placement 落点规则，目前仅支持 `block-surface`。
 * @property requireLineOfSight 是否要求施法者眼睛与落点之间视线无遮挡。
 */
class PositionPickDefinition(
    override val maxDurationTicks: Int,
    val range: Double,
    val placement: String,
    val requireLineOfSight: Boolean,
) : PickDefinition {

    override val type = TYPE

    override fun fields(): Map<String, Any?> {
        return mapOf(
            "range" to range,
            "placement" to placement,
            "requireLineOfSight" to requireLineOfSight,
        )
    }

    companion object {

        /** 类型标识。 */
        const val TYPE = "position"
    }
}
