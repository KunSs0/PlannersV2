package com.gitee.planners.module.particle

abstract class Particle {

    /**
     * Display the particle asynchronously
     *
     * @param x the x coordinate of the particle
     * @param y the y coordinate of the particle
     * @param z the z coordinate of the particle
     * @param world the world of the particle if any
     * @param lifetime the lifetime of the particle if any
     * @param size the size of the particle if any
     * @param alpha the alpha of the particle if any
     * @return void
     */
    abstract fun display(x: Double, y: Double, z: Double, world: Any? = null,
                         lifetime: Int = 1, size: Double = 1.0, alpha: Double = 1.0)

}