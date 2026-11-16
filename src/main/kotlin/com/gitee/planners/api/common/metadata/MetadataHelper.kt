package com.gitee.planners.api.common.metadata

import com.gitee.planners.util.unboxJavaToKotlin

/** 游戏 tick 与毫秒的固定换算比例：1 tick = 50 毫秒。 */
private const val MILLIS_PER_TICK = 50L

/**
 * 创建元数据。
 *
 * 有效期以游戏 tick 表达，写入时统一换算成毫秒绝对时刻，
 * 使在线判断与数据库落库共用同一个时间基准。
 *
 * @param data 元数据值；为 null 时返回已过期的空元数据。
 * @param timeoutTicks 有效期（tick），-1 表示永久有效。
 * @return 带失效时刻的元数据。
 */
fun metadataValue(data: Any?, timeoutTicks: Long = -1): MetadataTypeToken.TypeToken {
    if (data == null) {
        return MetadataTypeToken.Void()
    }
    // 限制有效期的最小值，负数一律视为永久有效。
    val timeout = maxOf(timeoutTicks, -1L)

    // 检查数据类型
    if (data !is Metadata.Unsaved && !Metadata.Loader.isSupported(unboxJavaToKotlin(data::class.java))) {
        throw IllegalStateException("Metadata type ${data::class.java} is not supported.")
    }
    val stopTime = if (timeout == -1L) {
        -1L
    } else {
        System.currentTimeMillis() + timeout * MILLIS_PER_TICK
    }

    return MetadataTypeToken.TypeToken(data::class.java, data, stopTime)
}
