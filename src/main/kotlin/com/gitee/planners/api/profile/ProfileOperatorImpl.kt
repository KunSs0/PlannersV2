package com.gitee.planners.api.profile

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerRoute
import java.util.concurrent.CompletableFuture

class ProfileOperatorImpl(val profile: PlayerProfile) : ProfileOperator {

    override fun clearRoute() {
        this.profile.route = null
        Database.INSTANCE.updateRoute(this.profile)
    }

    override fun setRoute(route: ImmutableRoute): CompletableFuture<Void> {

        return Database.INSTANCE.createPlayerJob(profile, -1, route).thenAccept {
            this.profile.route = it
            Database.INSTANCE.updateRoute(this.profile)
        }
    }

    override fun transferRoute(route: ImmutableRoute): CompletableFuture<Void> {
        return Database.INSTANCE.createPlayerJob(profile, profile.route?.bindingId ?: -1L, route).thenAccept {
            this.profile.route = it
            Database.INSTANCE.updateRoute(this.profile)
        }
    }

    override fun getRoute(): PlayerRoute? {
        return profile.route
    }

    override fun setMetadata(id: String, metadata: Metadata) {
        TODO("Not yet implemented")
    }
}
