package com.gitee.planners.api.common.metadata

import com.gitee.planners.util.unboxJavaToKotlin
import kotlin.math.min


fun createMetadata(data: Any, timeout: Long = -1): MetadataTypeToken.TypeToken {
    return data.metadata(timeout)
}

@Suppress("NAME_SHADOWING")
fun Any?.metadata(timeout: Long = -1): MetadataTypeToken.TypeToken {
    // 限制timeout的最小值
    val timeout = maxOf(timeout, -1L)
    // 如果是null 则代表要删除metadata 直接返回空
    if (this == null) {
        return MetadataTypeToken.Void()
    }
    if (!Metadata.Loader.isSupported(unboxJavaToKotlin(this::class.java))) {
        throw IllegalStateException("Metadata type ${this::class.java} is not supported.")
    }

    val stopTime = if (timeout == -1L) {
        -1L
    } else {
        System.currentTimeMillis() + timeout
    }

    return MetadataTypeToken.TypeToken(this::class.java, this, stopTime)
}
