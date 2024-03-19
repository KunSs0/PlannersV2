package com.gitee.planners.module.kether.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.particle.ParticleSpawnRegistry
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.*
import com.gitee.planners.module.particle.BukkitParticle
import com.gitee.planners.module.particle.shape.Circle
import com.gitee.planners.module.particle.shape.Line
import com.gitee.planners.module.particle.shape.ParticleShape
import com.gitee.planners.module.particle.shape.Point
import org.bukkit.Bukkit

@KetherEditor.Document("particle ...")
@CombinationKetherParser.Used
object ActionParticle : MultipleKetherParser("particle") {

    @KetherEditor.Document(
        value = "particle create <type particle:string> <shape:string> [frame:bool(false)] [duration:number(100)] [at objective:TargetContainer(origin)]",
        result = BukkitParticle::class
    )
    val create = KetherHelper.combinedKetherParser("create") {
        it.group(
            text(),
            text(),
            commandBool("frame", false),
            commandInt("duration", 100),
            commandObjectiveOrOrigin()
        ).apply(it) { type, shape, frame, duration, origin ->
            val nameSplit = type.split(":") // e.g.: minecraft:flame
            // Get particle type. TODO: Add Germ particles
            val (particle, id) = if (nameSplit.size == 1) {
                ParticleSpawnRegistry.getDefault() to nameSplit[0]
            } else {
                ParticleSpawnRegistry.get(nameSplit[0]) to nameSplit[1]
            }
            val bukkitParticle = BukkitParticle(
                particle,
                id,
                origin.filterIsInstance<TargetLocation<*>>().firstOrNull() ?: error("No location available")
            )
            bukkitParticle.frame.set(frame)
            bukkitParticle.duration.set(duration)
            if (shape != "")
                bukkitParticle.addShape(createParticleShape(shape))

            // TODO support metadata map

            now {
                bukkitParticle
            }

        }
    }

    @KetherEditor.Document("particle show <animated:BukkitParticle>")
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

    @KetherEditor.Document("particle stop <animated:particle animated>")
    val pause = KetherHelper.combinedKetherParser("pause","stop") {
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

    fun createParticleShape(name: String): ParticleShape {
        return when (name) {
            "point" -> Point()
            "line" -> Line()
            "circle" -> Circle()
            else -> error("Unknown particle shape $name")
        }
    }
}
