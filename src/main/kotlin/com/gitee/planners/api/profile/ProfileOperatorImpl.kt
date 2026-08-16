package com.gitee.planners.api.template

import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import java.util.concurrent.CompletableFuture

class ProfileOperatorImpl : ProfileOperator {

    override fun createPlayerRoute(template: PlayerTemplate, route: ImmutableRoute): CompletableFuture<PlayerRoute> {
        var playerRouter = template.playerRouter
        if (playerRouter == null) {
            val levelAlgorithm = com.gitee.planners.api.Registries.ROUTER.getOrNull(route.routerId)?.algorithmLevel
            var initialLevel = 1
            if (levelAlgorithm != null) {
                initialLevel = levelAlgorithm.minLevel
            } else {
                val defaultAlgorithm = AlgorithmLevel.default
                if (defaultAlgorithm != null) {
                    initialLevel = defaultAlgorithm.minLevel
                }
            }
            playerRouter = Database.INSTANCE.createPlayerRouter(template.id, route.routerId, initialLevel)
            template.playerRouter = playerRouter
            return Database.INSTANCE.createPlayerRoute(playerRouter, -1L, route).thenApply { playerRoute ->
                playerRouter.registerRoute(playerRoute)
                playerRouter.setCurrentRoute(playerRoute)
                playerRoute
            }
        }

        if (playerRouter.routerId != route.routerId) {
            throw IllegalArgumentException("玩家已选择 Router '${playerRouter.routerId}'")
        }
        var allowed = false
        for (nextRoute in playerRouter.getNextRoutes()) {
            if (nextRoute.id == route.id) {
                allowed = true
                break
            }
        }
        if (!allowed) {
            throw IllegalArgumentException("Job '${route.id}' 不是当前 Job 的可转职目标")
        }
        val parentId = playerRouter.currentRoute.bindingId
        return Database.INSTANCE.createPlayerRoute(playerRouter, parentId, route).thenApply { playerRoute ->
            playerRouter.registerRoute(playerRoute)
            playerRouter.setCurrentRoute(playerRoute)
            playerRoute
        }
    }
}
