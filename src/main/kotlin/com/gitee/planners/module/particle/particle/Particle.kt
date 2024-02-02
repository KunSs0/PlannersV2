package com.gitee.planners.module.particle.particle

import taboolib.common.util.Location
import taboolib.common.util.Vector

abstract class Particle(val namespace: Array<String>) {


    /**
     * Display the particle asynchronously
     *
     * @param particleId the id of the particle
     * @param location the location of the particle
     * @param lifetime the lifetime of the particle if any
     * @param count the count of the particle if any
     * @param size the size of the particle if any
     * @param alpha the alpha of the particle if any
     * @param offset the offset of the particle if any
     * @return void
     */
    fun spawn(particleId: String, location: Location, lifetime: Int = 1,
              count: Int = 1, size: Double = 1.0, alpha: Double = 1.0, speed: Double = 0.0,
              offset: Vector = Vector(0, 0, 0)) {
        spawn(particleId, location.world!!, location.x, location.y, location.z, lifetime, count, size, alpha, speed, offset)
    }

    /**
     * Display the particle asynchronously
     */
    abstract fun spawn(particleId: String, world: String, x: Double, y: Double, z: Double, lifetime: Int = 1,
                       count: Int = 1, size: Double = 1.0, alpha: Double = 1.0, speed: Double = 0.0,
                       offset: Vector = Vector(0, 0, 0))
}