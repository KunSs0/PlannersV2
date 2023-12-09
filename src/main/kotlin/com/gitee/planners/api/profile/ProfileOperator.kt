package com.gitee.planners.api.profile

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.player.PlayerRoute
import java.util.concurrent.CompletableFuture

interface ProfileOperator {

    fun clearRoute()

    fun setRoute(route: ImmutableRoute): CompletableFuture<Void>

    fun transferRoute(route: ImmutableRoute): CompletableFuture<Void>

    fun getRoute(): PlayerRoute?

    fun setMetadata(id: String, metadata: Metadata)

}
