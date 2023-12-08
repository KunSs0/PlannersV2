package com.gitee.planners.api.job.context

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender

class TargetConsoleCommandSender(val console : ConsoleCommandSender) : TargetCommandSender<ConsoleCommandSender> {

    override fun sendMessage(message: String) {
        console.sendMessage(message)
    }

    override fun dispatchCommand(command: String): Boolean {
        return Bukkit.dispatchCommand(console,command)
    }

    override fun getInstance(): CommandSender {
        return console
    }

}
