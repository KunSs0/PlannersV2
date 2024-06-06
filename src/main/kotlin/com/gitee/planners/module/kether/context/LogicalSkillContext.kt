package com.gitee.planners.module.kether.context

import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class LogicalSkillContext(sender: TargetBukkitEntity,val playerSkill : PlayerSkill) : ImmutableSkillContext(sender,playerSkill.immutable,playerSkill.level) {

    override val trackId = playerSkill.immutable.id

    val bukkitPlayer = sender.instance as? Player

    val isSupported = bukkitPlayer != null

    override fun call(): CompletableFuture<Any> {
        if (!isSupported) {
            error("Target sender $sender is not supported processing")
        }
        return super.call()
    }



}
