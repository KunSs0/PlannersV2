package com.gitee.planners.core.database

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerJob
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class DatabaseSQL : Database {
    override fun getPlayerProfile(player: Player): PlayerProfile {
        TODO("Not yet implemented")
    }

    override fun updateMetadata(profile: PlayerProfile, metadata: Metadata) {
        TODO("Not yet implemented")
    }

    override fun createPlayerSkill(profile: PlayerProfile, skill: ImmutableSkill): CompletableFuture<PlayerSkill> {
        TODO("Not yet implemented")
    }

    override fun createPlayerJob(profile: PlayerProfile, job: ImmutableJob): CompletableFuture<PlayerJob> {
        TODO("Not yet implemented")
    }


}
