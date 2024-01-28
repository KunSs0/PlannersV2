package com.gitee.planners.core.action.context

import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player

class LogicalSkillContext(sender: TargetBukkitEntity,val playerSkill : PlayerSkill) : ImmutableSkillContext(sender,playerSkill.immutable,playerSkill.level) {

    override val trackId = playerSkill.immutable.id

    val bukkitPlayer = sender.getInstance() as? Player

    val isSupported = bukkitPlayer != null

    override fun run() {
        if (!isSupported) {
            error("Target sender $sender is not supported processing")
        }

        super.run()
    }


}
