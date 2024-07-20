package com.gitee.planners.api.job.target

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Entity

inline fun <reified T : Target<*>> adaptTarget(any: Any): T {
    return when (any) {

        is CommandSender -> {
            any.adaptTarget() as T
        }

        is Location -> {
            any.adaptTarget() as T
        }

        is Block -> {
            TargetBlock(any) as T
        }

        else -> throw IllegalStateException("Target ${any::class.java.name} is not supported")
    }

}

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
