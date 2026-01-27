package com.gitee.planners.api.job.target

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * Target 统一工厂对象
 */
object Targets {

    fun of(entity: Entity): TargetBukkitEntity {
        return TargetBukkitEntity(entity)
    }

    fun of(player: Player): TargetBukkitEntity {
        return TargetBukkitEntity(player)
    }

    fun of(location: Location): TargetBukkitLocation {
        return TargetBukkitLocation(location)
    }

    fun of(location: taboolib.common.util.Location): TargetTabooLocation {
        return TargetTabooLocation(location)
    }

    fun of(block: Block): TargetBlock {
        return TargetBlock(block)
    }

    fun of(sender: ConsoleCommandSender): TargetConsoleCommandSender {
        return TargetConsoleCommandSender(sender)
    }

    fun of(sender: CommandSender): Target<*> {
        return when (sender) {
            is ConsoleCommandSender -> TargetConsoleCommandSender(sender)
            is Entity -> TargetBukkitEntity(sender)
            else -> throw IllegalStateException("Target ${sender::class.java.name} is not supported")
        }
    }

    /**
     * 从任意对象创建 Target（自动分发）
     */
    fun of(any: Any): Target<*> {
        return when (any) {
            is CommandSender -> of(any)
            is Location -> TargetBukkitLocation(any)
            is Block -> TargetBlock(any)
            is taboolib.common.util.Location -> TargetTabooLocation(any)
            else -> throw IllegalStateException("Target ${any::class.java.name} is not supported")
        }
    }
}

// 扩展函数语法糖
fun Entity.asTarget(): TargetBukkitEntity = Targets.of(this)
fun Player.asTarget(): TargetBukkitEntity = Targets.of(this)
fun Location.asTarget(): TargetBukkitLocation = Targets.of(this)
fun taboolib.common.util.Location.asTarget(): TargetTabooLocation = Targets.of(this)
fun Block.asTarget(): TargetBlock = Targets.of(this)
fun ConsoleCommandSender.asTarget(): TargetConsoleCommandSender = Targets.of(this)
fun CommandSender.asTarget(): Target<*> = Targets.of(this)
