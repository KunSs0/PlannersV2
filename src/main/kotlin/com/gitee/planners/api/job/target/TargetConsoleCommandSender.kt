package com.gitee.planners.api.job.target

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.core.config.State
import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender

class TargetConsoleCommandSender(val console: ConsoleCommandSender) : TargetCommandSender<ConsoleCommandSender>,
    TargetContainerization, CapableState {

    override val instance = console

    override fun sendMessage(message: String) {
        console.sendMessage(message)
    }

    override fun dispatchCommand(command: String): Boolean {
        return Bukkit.dispatchCommand(console, command)
    }

    override fun getMetadata(id: String): Metadata? {
        return container[id]
    }

    override fun setMetadata(id: String, data: Metadata) {
        container[id] = data
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun addState(state: State, duration: Long, coverBefore: Boolean) {

    }

    override fun removeState(state: State) {
        TODO("Not yet implemented")
    }

    override fun hasState(state: State): Boolean {
        TODO("Not yet implemented")
    }

    override fun isExpired(state: State): Boolean {
        TODO("Not yet implemented")
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
