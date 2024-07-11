package com.gitee.planners.api.template

import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import java.util.concurrent.CompletableFuture

interface ProfileOperator {

    fun createPlayerRoute(template: PlayerTemplate, route: ImmutableRoute) : CompletableFuture<PlayerRoute>

    fun createPlayerSkill(template: PlayerTemplate, skill: ImmutableSkill) : CompletableFuture<PlayerSkill>

}
