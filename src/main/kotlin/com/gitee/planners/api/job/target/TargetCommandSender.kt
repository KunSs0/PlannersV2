package com.gitee.planners.api.job.target

import org.bukkit.command.CommandSender

interface TargetCommandSender<T : CommandSender> : Target<T> {

    fun sendMessage(message: String)

    fun dispatchCommand(command: String): Boolean

}

