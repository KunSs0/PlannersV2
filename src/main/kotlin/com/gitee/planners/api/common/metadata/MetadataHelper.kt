package com.gitee.planners.api.common.metadata

fun Any.metadata(timeout: Long = -1): MetadataTypeToken.TypeToken {
    if (!Metadata.Loader.isSupported(this::class.java)) {
        throw IllegalStateException("Metadata type ${this::class.java} is not supported.")
    }

    val stopTime = if (timeout == -1L) {
        -1L
    } else {
        System.currentTimeMillis() + timeout
    }

    return MetadataTypeToken.TypeToken(this::class.java, this, stopTime)
}
