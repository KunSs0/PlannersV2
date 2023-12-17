package com.gitee.planners.api.job.context

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player

class LogicalSkillContext(sender: TargetBukkitEntity,val playerSkill : PlayerSkill) : ImmutableSkillContext(sender,playerSkill.immutable,playerSkill.level) {

    override val trackId = playerSkill.immutable.id

    val bukkitPlayer = sender.getInstance() as? Player

    val isSupported = bukkitPlayer != null

    override fun process() {
        if (!isSupported) {
            error("Target sender $sender is not supported processing")
        }

        super.process()
    }


}
