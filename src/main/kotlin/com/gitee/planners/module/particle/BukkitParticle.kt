package com.gitee.planners.module.particle

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.module.particle.animation.ParticleAnimation
import com.gitee.planners.module.particle.particle.ParticleSpawner
import com.gitee.planners.module.particle.shape.ParticleShape
import com.gitee.planners.util.math.asScaleMatrix
import com.gitee.planners.util.math.asVector
import org.ejml.simple.SimpleMatrix
import taboolib.common.platform.function.submit
import taboolib.common.util.Location
import taboolib.common.util.Vector
import taboolib.common5.cint
import kotlin.math.abs

@Suppress("UNREACHABLE_CODE")
open class BukkitParticle(val spawner: ParticleSpawner, particleId: String, location: ProxyTarget.Location<*>) {

    private val shapes = mutableListOf<ParticleShape>()

    private val animations = mutableListOf<ParticleAnimation>()

    private var bakedShape = SimpleMatrix(0, 4)

    private var framework: BukkitParticleFramework? = null

    private val lock = Any()

    private var isPlaying = false

    /** Is the shape baked */
    private var baked = false

    /** The duration of the animation */
    var duration: Int = 20

    /** Current tick */
    var tick: Int = 0

    /** The tick period */
    var period: Int = 1

    /** The speed of the animation. Negative values are allowed */
    var speed: Double = 1.0

    /** Is the particle animated, or show all frames at once */
    var frame: Boolean = false

    /** The origin */
    var origin: Vector = location.asVector()
        set(value) {
            field = value
            baked = false
        }

    var world: String = location.getWorld()

    val particleOffsetVector = Vector(0, 0, 0)

    var particleOffset: Double = 0.0
        set(value) {
            field = value
            particleOffsetVector.x = value
            particleOffsetVector.y = value
            particleOffsetVector.z = value
        }

    /** The lifetime of each particle */
    var particleLife: Int = 20

    var particleId: String = particleId

    /** The particle density per block unit */
    var particleDensity: Int = 10
        set(value) {
            field = value
            baked = false
        }

    var particleCount: Int = 1

    /**
     * Bake the shapes into a single matrix
     */
    private fun bakeShapes() {
        if (!baked) {
            var shape: SimpleMatrix? = null
            for (s in shapes) {
                val matrix = s.getShape(particleDensity)
                shape = if (shape == null) matrix else shape.concatRows(matrix)
            }
            bakedShape = shape ?: SimpleMatrix(0, 4)
            bakedShape = bakedShape.mult(origin.asScaleMatrix())
            baked = true
        }
    }

    /**
     * Partial function for spawning the particles
     */
    private fun spawn(x: Double, y: Double, z: Double) {
        spawner.spawn(
            particleId = particleId,
            world = world,
            x = x,
            y = y,
            z = z,
            lifetime = particleLife,
            count = particleCount,
            size = 0.0,
            alpha = 0.0,
            speed = 0.0,
            offset = particleOffsetVector
        )
    }

    fun start() = synchronized(lock) {
        if (isPlaying)
            error("The animation is already playing! consider pausing it before displaying it or make a copy of it.")

        TODO("Particle implementation is not complete yet.")

        // No frame animation
        if (!frame) {
            bakeShapes()
            for (i in 0 until bakedShape.numRows()) {
                val vector = bakedShape.extractVector(true, i) ?: break
                val displayLocation = Location(world, vector.get(0), vector.get(1), vector.get(2))
                spawner.spawn(particleId, displayLocation, particleLife)
            }
            return@synchronized
        }

        if (speed == 0.0) {
            error("The speed of the animation cannot be 0")
        }

        if (framework == null) {
            bakeShapes()
            framework = BukkitParticleFramework(
                animations,
                bakedShape,
                tick,
                (duration / abs(speed)).cint,
                backward = speed < 0,
                this::spawn
            )
            baked = false
        }
        isPlaying = true
        submit(async = true, period = period.toLong(), delay = 1) {
            synchronized(lock) {
                if (!isPlaying) {
                    cancel()
                    return@submit
                }
                if (framework!!.nextFrame()) {
                    framework = null
                    isPlaying = false
                    cancel()
                }
            }
        }
    }

    fun pause() = synchronized(lock) {
        isPlaying = false
    }

    fun reset() = synchronized(lock) {
        if (isPlaying) {
            error("The animation is playing, please pause it before resetting it.")
        }
        tick = 0
        framework = null
    }

    fun addAnimation(animation: ParticleAnimation) = synchronized(lock) {
        if (isPlaying) {
            error("The animation is playing, please pause it before adding a new animation.")
        }
        animations.add(animation)
    }

    fun clearAnimations() = synchronized(lock) {
        if (isPlaying) {
            error("The animation is playing, please pause it before clearing the animations.")
        }
        animations.clear()
    }

    fun addShape(shape: ParticleShape) = synchronized(lock) {
        if (isPlaying) {
            error("The animation is playing, please pause it before adding a new shape.")
        }
        shapes.add(shape)
    }

    fun clearShapes() = synchronized(lock) {
        if (isPlaying) {
            error("The animation is playing, please pause it before clearing the shapes.")
        }
        shapes.clear()
    }

    fun clone() = synchronized(lock) {
        val location = Location(world, origin.x, origin.y, origin.z)
        val clone = BukkitParticle(spawner, particleId, location.asTarget())
        clone.shapes.addAll(shapes)
        clone.animations.addAll(animations)
        clone.duration = duration
        clone.tick = tick
        clone.period = period
        clone.speed = speed
        clone.frame = frame
        clone.particleLife = particleLife
        clone.particleDensity = particleDensity
        clone.particleCount = particleCount
        clone.particleOffset = particleOffset
        clone
    }
}
