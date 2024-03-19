package com.gitee.planners.util

import com.gitee.planners.api.common.util.DefaultSynchronousSampling
import taboolib.common.platform.function.submit
import java.util.concurrent.CompletableFuture


fun <T> syncing(block: () -> T): DefaultSynchronousSampling<T> {
    return DefaultSynchronousSampling(block)
}
