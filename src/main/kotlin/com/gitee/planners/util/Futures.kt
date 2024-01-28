package com.gitee.planners.util

import taboolib.common.platform.function.submitAsync
import java.util.concurrent.CompletableFuture

fun <T> createBukkitAsyncFuture(future: CompletableFuture<T>) : CompletableFuture<T> {
    val f1 = CompletableFuture<T>()
    submitAsync {
        future.thenAccept {
            f1.complete(it)
        }
    }
    return f1
}
