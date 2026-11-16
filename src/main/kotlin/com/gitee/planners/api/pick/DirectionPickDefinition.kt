package com.gitee.planners.api.pick

/**
 * `direction` 指向定义：以方向角为结果，不绑定实体或世界位置。
 *
 * @property maxDurationTicks 会话最长保持 tick 数。
 * @property maxPitchDegrees 允许的最大俯仰角。
 */
class DirectionPickDefinition(
    override val maxDurationTicks: Int,
    val maxPitchDegrees: Double,
) : PickDefinition {

    override val type = TYPE

    override fun fields(): Map<String, Any?> {
        return mapOf(
            "maxPitchDegrees" to maxPitchDegrees,
        )
    }

    companion object {

        /** 类型标识。 */
        const val TYPE = "direction"
    }
}
