package com.gitee.planners.core.action.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.action.commandObjective
import org.bukkit.entity.Player
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

@KetherEditor.Document("sound <source> [with <volume(1)> <pitch(1)>] [at objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
object ActionSound : SimpleKetherParser("sound") {
    override fun run(): QuestActionParser {
        return combinationParser {
            it.group(
                text(),
                command("with", then = float().and(float())).option().defaultsTo(Pair(1f, 1f)),
                commandObjective(LeastType.SENDER)
            ).apply(it) { source, with, objective ->
                now {
                    val volume = with.first
                    val pitch = with.second
                    val sound = parseSound(source)
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        sound.play(it.entity as? Player ?: return@forEach,volume, pitch)
                    }
                }
            }
        }
    }

    fun parseSound(source: String): Sound {
        return if (source.startsWith("resource:")) {
            ResourceSound(source)
        } else {
            BukkitSound(source)
        }
    }

    interface Sound {

        fun play(player: Player, volume: Float, pitch: Float)

    }

    class ResourceSound(source: String) : Sound {

        private val source = source.substring(9)

        override fun play(player: Player, volume: Float, pitch: Float) {
            player.playSound(player.location, source, volume, pitch)
        }

    }

    class BukkitSound(source: String) : Sound {

        private val source = org.bukkit.Sound.valueOf(source.replace(".","_").uppercase())

        override fun play(player: Player, volume: Float, pitch: Float) {
            player.playSound(player.location, source, volume, pitch)
        }

    }

}
