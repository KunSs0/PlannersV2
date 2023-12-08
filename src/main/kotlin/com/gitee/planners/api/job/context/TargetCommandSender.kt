package com.gitee.planners.api.job.context

import org.bukkit.command.CommandSender

interface TargetCommandSender<T : CommandSender> : Target<CommandSender> {

    fun sendMessage(message: String)

    fun dispatchCommand(command: String): Boolean

}

