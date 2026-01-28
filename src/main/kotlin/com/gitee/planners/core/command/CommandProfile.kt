package com.gitee.planners.core.command

import com.gitee.planners.core.command.profile.CommandExperience
import com.gitee.planners.core.command.profile.CommandLevel
import com.gitee.planners.core.command.profile.CommandMagicPoint
import taboolib.common.platform.command.CommandBody

object CommandProfile {

    @CommandBody
    val level = CommandLevel

    @CommandBody
    val experience = CommandExperience

    @CommandBody(aliases = ["mp"])
    val magicpoint = CommandMagicPoint
}
