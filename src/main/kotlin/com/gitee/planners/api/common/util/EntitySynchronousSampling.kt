package com.gitee.planners.api.common.util

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Entity
import taboolib.common.platform.function.submit
import java.util.concurrent.CompletableFuture

class EntitySynchronousSampling(val world: World) {

    fun get() : List<Entity> {
        if (Bukkit.isPrimaryThread()) {
            return world.entities
        }
        val future = CompletableFuture<List<Entity>>()
        submit {
            future.complete(world.entities)
        }
        return future.join()
    }

}
