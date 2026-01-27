package com.gitee.planners.module.particle

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.module.particle.animation.ParticleAnimated
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
open class BukkitParticle(val spawner: ParticleSpawner, particleId: String, location: ProxyTarget.Location<*>) : AbstractAnimated(), Animated.Periodic {

    private val shapes = mutableListOf<ParticleShape>()

    private val animations = mutableListOf<ParticleAnimated>()

    private var bakedShape = SimpleMatrix(0, 4)

    private var framework: BukkitParticleFramework? = null

    private val lock = Any()

    private var isPlaying = false

    /** Is the shape baked */
    private var baked = false

    /** The duration of the animation */
    val duration = strictInt("duration", 20, 1) { }

    /** Current tick */
    val tick = strictInt("tick", 0) { }

    /** The tick period */
    val period = strictInt("period", 1, 1) { }

    /** Ths speed of the animation. Negative values are allowed */
    val speed = double("speed", 1.0) { }

    /** Is the particle animated, or show all frames at once */
    val frame = bool("frame", false) { }

    /** The origin */
    val origin = vector("origin", location.asVector()) {
        baked = false
    }

    val world = text("world", location.getWorld()) { }

    val particleOffsetVector = Vector(0, 0, 0)

    val particleOffset = double("p-offset", 0.0) {
        particleOffsetVector.x = it
        particleOffsetVector.y = it
        particleOffsetVector.z = it
    }

    /** The lifetime of each particle */
    val particleLife = strictInt("p-life", 20, 1) { }

    val particleId = text("p-id", particleId) { }

    /** The particle density per block unit */
    val particleDensity = strictInt("p-density", 10) {
        baked = false
    }

    val particleCount = strictInt("p-count", 1) { }

    /** Current tick */
    override val timestampTick: Long
        get() = tick.asLong()

    /**
     * Bake the shapes into a single matrix
     */
    private fun bakeShapes() {
        if (!baked) {
            var shape: SimpleMatrix? = null
            for (s in shapes) {
                val matrix = s.getShape(particleDensity.asInt())
                shape = if (shape == null) matrix else shape.concatRows(matrix)
            }
            bakedShape = shape ?: SimpleMatrix(0, 4)
            // Add origin transformation
            bakedShape = bakedShape.mult(origin.asScaleMatrix())
            baked = true
        }
    }

    /**
     * Partial function for spawning the particles
     */
    private fun spawn(x: Double, y: Double, z: Double) {
        spawner.spawn(
            particleId = particleId.asString(),
            world = world.asString(),
            x = x,
            y = y,
            z = z,
            lifetime = particleLife.asInt(),
            count = particleCount.asInt(),
            size = 0.0,
            alpha = 0.0,
            speed = 0.0,
            offset = particleOffsetVector
        ) // TODO: Add size and alpha
    }

    fun start() = synchronized(lock) {
        if (isPlaying)
            error("The animation is already playing! consider pausing it before displaying it or make a copy of it.")

        TODO("Particle implementation is not complete yet.")

        // No frame animation
        if (!frame.asBoolean()) {
            bakeShapes()
            for (i in 0 until bakedShape.numRows()) {
                val vector = bakedShape.extractVector(true, i) ?: break
                val displayLocation = Location(world.asString(), vector.get(0), vector.get(1), vector.get(2))
                spawner.spawn(particleId.asString(), displayLocation, particleLife.asInt())
            }
            return@synchronized
        }

        if (speed.asDouble() == 0.0) {
            error("The speed of the animation cannot be 0")
        }

        if (framework == null) {
            bakeShapes()
            // Restore the animation
            framework = BukkitParticleFramework(
                animations,
                bakedShape, // copy will be made
                tick.asInt(),
                (duration.asDouble() / abs(speed.asDouble())).cint,
                backward = speed.asDouble() < 0,
                this::spawn
            )
            baked = false
        }
        // Play the animation asynchronously
        isPlaying = true
        submit(async = true, period = period.asLong(), delay = 1) {
            synchronized(lock) {
                if (!isPlaying) {
                    cancel()
                    return@submit
                }
                if (framework!!.nextFrame()) { // finished
                    framework = null
                    isPlaying = false
                    cancel()
                }
            }
        }
    }

    fun pause() = synchronized(lock) {
        isPlaying = false // Will pause by next frame
    }

    fun reset() = synchronized(lock) {
        if (isPlaying) {
            error("The animation is playing, please pause it before resetting it.")
        }

        tick.set(0)
        framework = null
    }

    fun addAnimation(animation: ParticleAnimated) = synchronized(lock) {
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

    override fun handleUpdate(metadata: AnimatedMeta.CoerceMeta<Any>) = synchronized(lock) {
        if (isPlaying) {
            error("The animation is playing, please pause it before updating the metadata.")
        }
        super.handleUpdate(metadata)
    }

    fun clone() = synchronized(lock) {
        val vector = origin.asVector()
        val location = Location(world.asString(), vector.x, vector.y, vector.z)
        val clone = BukkitParticle(spawner, particleId.asString(), location.asTarget())
        clone.shapes.addAll(shapes)
        clone.animations.addAll(animations)
        this.copyMetaDataTo(clone)
        clone
    }
}
