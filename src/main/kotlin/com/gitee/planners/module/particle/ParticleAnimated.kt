package com.gitee.planners.module.particle

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import com.gitee.planners.module.particle.animation.Animation
import com.gitee.planners.module.particle.shape.Shape
import org.bukkit.Bukkit
import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector

class ParticleAnimated(val particle: Particle) : AbstractAnimated(), Animated.Periodic {

    private val shapes = mutableListOf<Shape>()

    private val animations = mutableListOf<Animation>()

    private var bakedShape = SimpleMatrix(0, 4)

    private val lock = Any()

    protected var isPlaying = false

    protected var baked = false


    /** The duration of the animation */
    val duration = strictInt("duration", 20, 1) { }

    /** Current tick */
    val tick = strictInt("tick", 0) { }

    /** Ths speed of the animation. Negative values are allowed */
    val speed = double("speed", 1.0) { }

    /** Is the particle animated, or show all frames at once */
    val animated = bool("animated", false) { }

    /** The lifetime of each particle */
    val life = strictInt("life", 20, 1) { }

    /** The particle density per block unit */
    val density = strictInt("density", 10) {
        baked = false
    }

    /** The origin */
    val origin = vector("origin", Vector(0, 0, 0)) {
        baked = false
    }

    /** Current tick */
    override val timestampTick: Long
        get() = tick.asLong()

    /**
     * Bake the shapes into a single matrix
     */
    private fun bakeShapes(): SimpleMatrix {
        // TODO: Add origin transformation and ctx support
        var shape: SimpleMatrix? = null
        for (s in shapes) {
            val matrix = s.getShape(density.asInt())
            shape = if (shape == null) matrix else shape.concatRows(matrix)
        }
        return shape ?: SimpleMatrix(0, 4)
    }

    /**
     * Animate the next frame
     */
    private fun nextFrame() {
        if (tick.asInt() >= duration.asInt()) {
            tick.set(0)
            isPlaying = false
        }

        // Transform the shape
        var frame = bakedShape
        for (anim in animations) {
            if (anim.start.asInt() < tick.asInt() && tick.asInt() < anim.end.asInt()) {
                frame = anim.play((tick.asInt() - anim.start.asInt()) / (anim.end.asInt() - anim.start.asInt()).toDouble())(frame)
            }
        }

        for (i in 0 until frame.numRows) {
            val vector = frame.extractVector(true, i) ?: break
            particle.display(vector.get(0), vector.get(1), vector.get(2), vector.get(3), life.asInt())
        }

        tick.set(tick.asInt() + 1)
    }

    fun display() = synchronized(lock) {
        if (!baked) {
            bakedShape = bakeShapes()
            baked = true
        }

        if (!animated.asBoolean()) {
            for (i in 0 until bakedShape.numRows) {
                val vector = bakedShape.extractVector(true, i) ?: break
                particle.display(vector.get(0), vector.get(1), vector.get(2), vector.get(3), life.asInt())
            }
        } else {
            TODO("Implement the animation")
        }
    }

    override fun handleUpdate(metadata: AnimatedMeta.CoerceMeta<Any>) = synchronized(lock) {
        if (isPlaying) {
            Bukkit.getLogger().warning("The animation is playing, please pause it before updating the metadata.")
        } else {
            super.handleUpdate(metadata)
        }
    }
}