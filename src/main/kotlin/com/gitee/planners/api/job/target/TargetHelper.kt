package com.gitee.planners.api.job.target

import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Entity

fun Entity.adaptTarget(): TargetBukkitEntity {
    return TargetBukkitEntity(this)
}

fun Location.adaptTarget(): TargetBukkitLocation {
    return TargetBukkitLocation(this)
}

fun taboolib.common.util.Location.adaptTarget(): TargetTabooLocation {
    return TargetTabooLocation(this)
}

fun CommandSender.adaptTarget(): Target<*> {
    return when (this) {
        is ConsoleCommandSender -> {
            TargetConsoleCommandSender(this)
        }

        is Entity -> {
            TargetBukkitEntity(this)
        }

        else -> throw IllegalStateException("Target ${this::class.java.name} is not supported")
    }
}
