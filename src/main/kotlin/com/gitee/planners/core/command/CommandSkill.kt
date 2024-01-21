package com.gitee.planners.core.command

import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.core.action.context.AbstractSkillContext
import com.gitee.planners.core.action.context.ImmutableSkillContext
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.SimpleCommandBody
import taboolib.platform.util.sendLang

object CommandSkill {

    @CommandBody
    val cast = Command.withPlayerSkill { player, skill ->
        val context = ImmutableSkillContext(player.adaptTarget(), skill.immutable, 1)
        context.process()
        player.sendMessage("casted ${skill.id}")
    }


}
