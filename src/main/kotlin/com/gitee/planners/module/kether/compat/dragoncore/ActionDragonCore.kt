package com.gitee.planners.module.kether.compat.dragoncore

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.actionVector
import com.gitee.planners.module.kether.commandObjective
import eos.moe.dragoncore.api.CoreAPI
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.platform.util.onlinePlayers
import java.util.*

object ActionDragonCore : MultipleKetherParser("dragoncore") {

    @KetherEditor.Document("dragoncore particle <scheme> [rotation: (0,0,0)] [tile: (100)] [at: TargetContainer(sender)]")
    val particle = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            actionVector(),
            int().optional(),
            commandObjective()
        ).apply(it) { scheme, pos, tile, objective ->
            now {
                objective.forEach {
                    val id = UUID.randomUUID().toString()
                    val posOrEntityId = when (it) {
                        is TargetBukkitEntity -> {
                            it.instance.uniqueId.toString()
                        }

                        is TargetLocation<*> -> {
                            "${it.getWorld()},${it.getX()},${it.getY()},${it.getZ()}"
                        }

                        else -> {
                            return@forEach
                        }
                    }
                    onlinePlayers.forEach {
                        PacketSender.addParticle(
                            it,
                            scheme,
                            id,
                            posOrEntityId,
                            "${pos.x},${pos.y},${pos.z}",
                            tile.orElseGet { 100 })
                    }

                }
            }
        }
    }

    @KetherEditor.Document("dragoncore sound <name> [id: (random.id)] [type: (music)] [volume: 1.0] [pitch: 1.0] [loop: (false)] [selector: TargetContainer(sender)]")
    val sound = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            text().optional(),
            text().optional(),
            float().optional(),
            float().optional(),
            bool().optional(),
            commandObjective()
        ).apply(it) { name, id, type, volume, pitch, loop, objective ->
            now {
                objective.filterIsInstance<TargetBukkitEntity>().forEach {
                    val entity = it.instance as? Player ?: return@forEach
                    PacketSender.sendPlaySound(
                        entity,
                        name,
                        id.orElseGet { "random.id" },
                        type.orElseGet { "music" },
                        volume.orElseGet { 1f },
                        pitch.orElseGet { 1f },
                        loop.orElseGet { false },
                        0f,
                        0f,
                        0f
                    )
                }
            }
        }
    }

    val playeranimation = object : MultipleKetherParser("playeranimation", "panimation") {

        @KetherEditor.Document("dragoncore playeranimation send <name> [selector: TargetContainer(sender)]")
        val send = KetherHelper.combinedKetherParser {
            it.group(text(), commandObjective()).apply(it) { name, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        PacketSender.setPlayerAnimation(entity, name)
                    }
                }
            }
        }

        @KetherEditor.Document("dragoncore playeranimation remove [selector: TargetContainer(sender)]")
        val remove = KetherHelper.combinedKetherParser {
            it.group(commandObjective()).apply(it) { objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        PacketSender.removePlayerAnimation(entity)
                    }
                }
            }
        }

        val stop = remove

        private fun process(func: (entity: Player, name: String) -> Unit) = KetherHelper.combinedKetherParser {
            it.group(text(), commandObjective()).apply(it) { name, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        func(entity, name)
                    }
                }
            }
        }
    }

    val animation = object : MultipleKetherParser("animation") {

        @KetherEditor.Document("dragoncore animation send <name> [transition: 0] [selector: TargetContainer(sender)]")
        val send = process { entity, name, transition ->
            CoreAPI.setEntityAnimation(entity, name, transition)
        }

        @KetherEditor.Document("dragoncore animation remove <name> [transition: 0] [selector: TargetContainer(sender)]")
        val remove = process { entity, name, transition ->
            CoreAPI.removeEntityAnimation(entity, name, transition)
        }

        val stop = remove

        private fun process(func: (entity: LivingEntity, name: String, transition: Int) -> Unit) =
            KetherHelper.combinedKetherParser {
                it.group(text(), int().optional(), commandObjective()).apply(it) { name, transition, objective ->
                    now {
                        objective.filterIsInstance<TargetBukkitEntity>().forEach {
                            val entity = it.instance as? LivingEntity ?: return@forEach
                            func(entity, name, transition.orElseGet { 0 })
                        }
                    }
                }
            }


    }

    val sync = object : MultipleKetherParser("sync") {

        @KetherEditor.Document("dragoncore sync send <map> [selector: TargetContainer(sender)]")
        val send =
            KetherHelper.combinedKetherParser {
                it.group(text(), commandObjective()).apply(it) { map, objective ->
                    now {
                        objective.filterIsInstance<TargetBukkitEntity>().forEach {
                            val player = it.instance as? Player ?: return@forEach
                            PacketSender.sendSyncPlaceholder(player, map.split(" ").associate { it.split(",").let { it[0] to it[1] } })
                        }
                    }
                }
            }

        @KetherEditor.Document("dragoncore sync delete <name> <isStartWith> [selector: TargetContainer(sender)]")
        val delete =
            KetherHelper.combinedKetherParser {
                it.group(text(), bool(), commandObjective()).apply(it) { name, isStartWith, objective ->
                    now {
                        objective.filterIsInstance<TargetBukkitEntity>().forEach {
                            val player = it.instance as? Player ?: return@forEach
                            PacketSender.sendDeletePlaceholderCache(player, name, isStartWith)
                        }
                    }
                }
            }

        val remove = delete

    }

    val entityfunction = object : MultipleKetherParser("entityfunction") {

        @KetherEditor.Document("dragoncore entityfunction <function> [selector: TargetContainer(sender)]")
        val send =
            KetherHelper.combinedKetherParser {
                it.group(text(), commandObjective()).apply(it) { function, objective ->
                    now {
                        objective.filterIsInstance<TargetBukkitEntity>().forEach {
                            val entity = it.instance as? LivingEntity ?: return@forEach
                            onlinePlayers.forEach {
                                PacketSender.runEntityAnimationFunction(it, entity.uniqueId, function)
                            }
                        }
                    }
                }
            }

    }


}
