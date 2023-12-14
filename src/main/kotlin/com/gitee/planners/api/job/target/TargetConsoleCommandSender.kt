package com.gitee.planners.api.job.target

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender

class TargetConsoleCommandSender(val console: ConsoleCommandSender) : TargetCommandSender<ConsoleCommandSender>,
    TargetContainerization {

    override fun sendMessage(message: String) {
        console.sendMessage(message)
    }

    override fun dispatchCommand(command: String): Boolean {
        return Bukkit.dispatchCommand(console, command)
    }

    override fun getInstance(): ConsoleCommandSender {
        return console
    }

    override fun getMetadata(id: String): Metadata? {
        return container[id]
    }

    override fun setMetadata(id: String, data: Metadata) {
        container[id] = data
    }

    override fun toString(): String {
        return "TargetConsoleCommandSender(console=$console)"
    }

    companion object {

        // 控制台元数据 在关服后数据丢失
        private val container = object : MetadataContainer(emptyMap()) {

        }

    }

}
