package com.gitee.planners.api.common.metadata

import com.gitee.planners.util.unboxJavaToKotlin


/**
 * 创建元数据
 *
 * @param data Any
 * @param timeout Long
 * @return Metadata
 */
fun metadataValue(data: Any?, timeout: Long = -1): MetadataTypeToken.TypeToken {
    if (data == null) {
        return MetadataTypeToken.Void()
    }
    // 限制timeout的最小值
    val timeout = maxOf(timeout, -1L)

    // 检查数据类型
    if (data !is Metadata.Unsaved && !Metadata.Loader.isSupported(unboxJavaToKotlin(data::class.java))) {
        throw IllegalStateException("Metadata type ${data::class.java} is not supported.")
    }
    val stopTime = if (timeout == -1L) {
        -1L
    } else {
        System.currentTimeMillis() + timeout
    }

    return MetadataTypeToken.TypeToken(data::class.java, data, stopTime)
}