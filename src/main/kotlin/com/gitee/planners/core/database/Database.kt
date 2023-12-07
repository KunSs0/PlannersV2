package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

interface Database {

    fun getPlayerProfile(player: Player): PlayerProfile

    fun updateMetadata(profile: PlayerProfile,id: String, metadata: Metadata)

    fun createPlayerSkill(profile: PlayerProfile, skill: ImmutableSkill): CompletableFuture<PlayerSkill>

    fun createPlayerJob(profile: PlayerProfile, job: ImmutableJob): CompletableFuture<PlayerRoute>

}
