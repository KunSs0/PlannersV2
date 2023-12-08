package com.gitee.planners.api.job.context

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import java.util.*

class TargetBukkitEntity(val entity: Entity) : TargetEntity<Entity>, TargetCommandSender<CommandSender> {
    override fun getUniqueId(): UUID {
        return entity.uniqueId
    }

    override fun getName(): String {
        return entity.name
    }

    override fun getInstance(): Entity {
        return entity
    }

    override fun getWorld(): String {
        return entity.world.name
    }

    override fun getBukkitWorld(): World? {
        return entity.world
    }

    override fun getX(): Double {
        return entity.location.x
    }

    override fun getY(): Double {
        return entity.location.y + entity.height / 2
    }

    override fun getZ(): Double {
        return entity.location.z
    }

    override fun sendMessage(message: String) {
        getInstance().sendMessage(message)
    }

    override fun dispatchCommand(command: String): Boolean {
        return Bukkit.dispatchCommand(getInstance(), command)
    }


}
