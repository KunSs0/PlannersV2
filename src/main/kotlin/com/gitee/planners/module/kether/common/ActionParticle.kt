package com.gitee.planners.module.kether.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.particle.ParticleRegistry
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.*
import com.gitee.planners.module.particle.ParticleAnimated
import com.gitee.planners.module.particle.shape.Circle
import com.gitee.planners.module.particle.shape.Line
import com.gitee.planners.module.particle.shape.ParticleShape
import com.gitee.planners.module.particle.shape.Point
import org.bukkit.Bukkit

@KetherEditor.Document("particle ...")
@CombinationKetherParser.Used
object ActionParticle : MultipleKetherParser("particle") {

    @KetherEditor.Document("particle new <type:particle> [at location:location] [with shape:particle shape] " +
            "[animated:bool] [duration:Number]", result = ParticleAnimated::class)
    val create = KetherHelper.combinedKetherParser("new") {
        it.group(text(),
                commandObjectiveOrOrigin(),
                commandText("shape", ""),
                commandBool("animated"),
                commandInt("duration", 100)).apply(it) { type, origin, shape, animated, duration ->
            val nameSplit = type.split(":") // e.g.: minecraft:flame
            // Get particle type. TODO: Add Germ particles
            val (particle, id) = if (nameSplit.size == 1) {
                ParticleRegistry.getDefault() to nameSplit[0]
            } else {
                ParticleRegistry.get(nameSplit[0]) to nameSplit[1]
            }
            val particleAnimated = ParticleAnimated(particle, id,
                    origin.filterIsInstance<TargetLocation<*>>().firstOrNull() ?: error("No location available"))
            particleAnimated.animated.set(animated)
            particleAnimated.duration.set(duration)
            if (shape != "")
                particleAnimated.addShape(newParticleShape(shape))

            // TODO support metadata map

            now {
                particleAnimated
            }

        }
    }

    @KetherEditor.Document("particle show <animated:particle animated>")
    val show = KetherHelper.combinedKetherParser("show", "display") {
        it.group(actionParticle()).apply(it) { particleAnimated ->
            now {
                try {
                    particleAnimated.start()
                } catch (e: Exception) {
                    Bukkit.getLogger().warning("Failed to start particle: ${e.message}")
                }
            }
        }
    }

    @KetherEditor.Document("particle hide <animated:particle animated>")
    val pause = KetherHelper.combinedKetherParser("pause") {
        it.group(actionParticle()).apply(it) { particleAnimated ->
            now {
                try {
                    particleAnimated.pause()
                } catch (e: Exception) {
                    Bukkit.getLogger().warning("Failed to pause particle: ${e.message}")
                }
            }
        }
    }

    @KetherEditor.Document("particle reset <animated:particle animated>")
    val reset = KetherHelper.combinedKetherParser("reset") {
        it.group(actionParticle()).apply(it) { particleAnimated ->
            now {
                try {
                    particleAnimated.reset()
                } catch (e: Exception) {
                    Bukkit.getLogger().warning("Failed to reset particle: ${e.message}")
                }
            }
        }
    }

    // TODO support shape

    fun newParticleShape(name: String): ParticleShape {
        return when (name) {
            "point" -> Point()
            "line" -> Line()
            "circle" -> Circle()
            else -> error("Unknown particle shape $name")
        }
    }
}