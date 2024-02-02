package com.gitee.planners.module.particle.particle

import org.bukkit.Bukkit
import taboolib.common.util.Vector

object MinecraftParticle : Particle(arrayOf("minecraft", "mc")) {

    override fun spawn(particleId: String, world: String, x: Double, y: Double, z: Double, lifetime: Int, count: Int, size: Double, alpha: Double, speed: Double, offset: Vector) {
        Bukkit.getWorld(world)!!.spawnParticle(org.bukkit.Particle.valueOf(particleId),
                x, y, z, count, offset.x, offset.y, offset.z, speed, null, true)
    }

}