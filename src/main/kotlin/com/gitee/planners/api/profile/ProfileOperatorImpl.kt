package com.gitee.planners.api.template

import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import java.util.concurrent.CompletableFuture

class ProfileOperatorImpl : ProfileOperator {

    override fun createPlayerRoute(template: PlayerTemplate, route: ImmutableRoute): CompletableFuture<PlayerRoute> {
        return Database.INSTANCE.createPlayerJob(template, -1, route)
    }

    override fun createPlayerSkill(template: PlayerTemplate, skill: ImmutableSkill): CompletableFuture<PlayerSkill> {
        return Database.INSTANCE.createPlayerSkill(template, skill)
    }
}
