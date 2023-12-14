package com.gitee.planners.api.common.metadata

import com.gitee.planners.util.unboxJavaToKotlin

fun Any?.metadata(timeout: Long = -1): MetadataTypeToken.TypeToken {
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
