package com.gitee.planners.module.kether

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.Target.Companion.cast
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.player.magic.MagicPoint.magicPoint
import com.gitee.planners.core.player.magic.MagicPoint.magicPointInUpperLimit
import org.bukkit.entity.Player
import java.util.Optional

@CombinationKetherParser.Used
object ActionProfile : MultipleKetherParser("profile") {

    @KetherEditor.Document("profile magicpoint")
    val magicPoint = object : MultipleKetherParser("mp") {

        @KetherEditor.Document("profile magicpoint get [at objective:TargetContainer(sender)]")
        val get = KetherHelper.combinedKetherParser("mp") {
            it.group(commandObjectiveOrSender()).apply(it) { objective ->
                now {
                    val entity = objective
                        .filterIsInstance<TargetBukkitEntity>()
                        .firstOrNull { it.instance is Player }
                    Optional.ofNullable((entity?.instance as? Player)?.plannersTemplate?.magicPoint).orElseGet { -1 }
                }
            }
        }

        @KetherEditor.Document("profile magicpoint [at objective:TargetContainer(sender)]")
        val main = get

        @KetherEditor.Document("profile magicpoint set <value> [at objective:TargetContainer(sender)]")
        val set = KetherHelper.combinedKetherParser("mp") {
            it.group(int(), commandObjectiveOrSender()).apply(it) { value, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val player = it.instance as? Player
                        if (player != null) {
                            player.plannersTemplate.magicPoint = value
                        }
                    }
                }
            }
        }

        @KetherEditor.Document("profile magicpoint add <value> [at objective:TargetContainer(sender)]")
        val add = KetherHelper.combinedKetherParser("mp") {
            it.group(int(), commandObjectiveOrSender()).apply(it) { value, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val player = it.instance as? Player
                        if (player != null) {
                            player.plannersTemplate.magicPoint += value
                        }
                    }
                }
            }
        }

        @KetherEditor.Document("profile magicpoint take <value> [at objective:TargetContainer(sender)]")
        val take = KetherHelper.combinedKetherParser("mp") {
            it.group(int(), commandObjectiveOrSender()).apply(it) { value, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val player = it.instance as? Player
                        if (player != null) {
                            player.plannersTemplate.magicPoint -= value
                        }
                    }
                }
            }
        }

    }

    @KetherEditor.Document("profile magicpoint.max [at objective:TargetContainer(sender)]")
    val magicPointInUpperLimit = KetherHelper.combinedKetherParser("mp.max", "magicpoint.max", "mp.upperlimit") {
        it.group(commandObjectiveOrSender()).apply(it) { objective ->
            now {
                val entity = objective
                    .filterIsInstance<TargetBukkitEntity>()
                    .firstOrNull { it.instance is Player }
                Optional.ofNullable((entity?.instance as? Player)?.plannersTemplate?.magicPointInUpperLimit).orElseGet { -1 }
            }
        }
    }

    @KetherEditor.Document("profile job")
    val job = processNow {
        it.plannersTemplate.route?.getJob()?.id
    }

    @KetherEditor.Document("profile level")
    val level = processNow {
        it.plannersTemplate.level
    }

    @KetherEditor.Document("profile experience")
    val experience = processNow {
        it.plannersTemplate.experience
    }

    @KetherEditor.Document("profile experience.max")
    val experienceMax = processNow("experience.max", "max.experience", "experience-max", "max-exp", "max.exp") {
        it.plannersTemplate.experienceMax
    }

    fun processNow(vararg id: String, func: (Player) -> Any?) = KetherHelper.simpleKetherNow(*id) {
        val sender = getEnvironmentContext().sender.cast<TargetBukkitEntity>()
        if (sender == null) {
            error("No sender selected.")
        }
        val player = sender.instance as? Player
        if (player == null) {
            error("Sender is not a player.")
        }
        func(player)
    }

}
