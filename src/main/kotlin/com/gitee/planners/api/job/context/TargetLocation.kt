package com.gitee.planners.api.job.context

import org.bukkit.Bukkit
import org.bukkit.World
import taboolib.common.util.Location


interface TargetLocation<T> : Target<T> {

    fun getWorld(): String

    fun getBukkitWorld(): World?

    fun getX(): Double

    fun getY(): Double

    fun getZ(): Double

}
