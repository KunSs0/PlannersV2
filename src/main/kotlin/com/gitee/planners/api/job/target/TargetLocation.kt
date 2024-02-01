package com.gitee.planners.api.job.target

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import taboolib.common.util.Vector


interface TargetLocation<T> : Target<T> {

    fun getWorld(): String

    fun getBukkitWorld(): World?

    fun getBukkitLocation() : Location


    fun getX(): Double

    fun getY(): Double

    fun getZ(): Double

    fun getNearbyLivingEntities(vector: Vector): List<LivingEntity>

}
