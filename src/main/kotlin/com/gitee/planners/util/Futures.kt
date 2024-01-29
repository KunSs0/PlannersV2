package com.gitee.planners.util

import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import java.util.concurrent.CompletableFuture

fun <T> createBukkitAwaitFuture(func: () -> T) : CompletableFuture<T> {
    val f1 = CompletableFuture<T>()
    submit {
        f1.complete(func())
    }
    return f1
}
