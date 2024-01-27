package com.gitee.planners.api.profile

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import java.util.concurrent.CompletableFuture

interface ProfileOperator {

    fun createPlayerRoute(profile: PlayerProfile,route: ImmutableRoute) : CompletableFuture<PlayerRoute>

    fun createPlayerSkill(profile: PlayerProfile,skill: ImmutableSkill) : CompletableFuture<PlayerSkill>

}
