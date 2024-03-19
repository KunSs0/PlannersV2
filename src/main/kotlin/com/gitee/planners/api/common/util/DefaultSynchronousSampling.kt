package com.gitee.planners.api.common.util

import org.bukkit.Bukkit
import taboolib.common.platform.function.submit
import java.util.concurrent.CompletableFuture

class DefaultSynchronousSampling<T>(val block: () -> T): SynchronousSampling<T> {

    override fun get(): T {
        if (Bukkit.isPrimaryThread()) {
            return block()
        }
        val future = CompletableFuture<T>()
        submit {
            future.complete(block())
        }
        return future.join()
    }

}
