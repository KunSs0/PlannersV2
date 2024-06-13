package com.gitee.planners.module.kether.germengine

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.SoundType
import com.germ.germplugin.api.ViewType
import com.germ.germplugin.api.bean.AnimDataDTO
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.commandObjective
import com.gitee.planners.module.kether.commandObjectiveOrEmpty
import com.gitee.planners.module.kether.common.ActionContext.sender
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common5.cfloat
import taboolib.type.BukkitEquipment
import java.util.*

@CombinationKetherParser.Used
object ActionGermEngine : MultipleKetherParser("germengine") {

    val effect = object : MultipleKetherParser("effect") {

        @KetherEditor.Document("germengine effect <name> [at objective:TargetContainer(sender)]")
        val send = KetherHelper.combinedKetherParser {
            it.group(text(), commandObjective()).apply(it) { name, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
//                        GermPacketAPI.sendEffect(entity, name)
                    }
                }
            }
        }

        @KetherEditor.Document("germengine effect clear [at objective:TargetContainer(sender)]")
        val clear = KetherHelper.combinedKetherParser {
            it.group(commandObjective()).apply(it) { objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.clearEffect(entity)
                    }
                }
            }
        }

    }

    val view = object : MultipleKetherParser("look") {

        @KetherEditor.Document("germengine view unlock [at objective:TargetContainer(sender)]")
        val unlock = KetherHelper.combinedKetherParser {
            it.group(commandObjective()).apply(it) { objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendUnlockPlayerCameraView(entity)
                    }
                }
            }
        }

        @KetherEditor.Document("germengine view [durationTick: -1] [type: (1)] [at objective:TargetContainer(sender)]")
        val lock = KetherHelper.combinedKetherParser {
            it.group(
                long().option().defaultsTo(-1),
                text().option().defaultsTo("1"),
                commandObjective()
            ).apply(it) { duration, type, objective ->
                now {
                    val viewType = when (type) {
                        "1" -> ViewType.FIRST_PERSON
                        "2" -> ViewType.THIRD_PERSON
                        "3" -> ViewType.THIRD_PERSON_REVERSE
                        else -> error("Invalid view type. in [1, 2, 3]")
                    }

                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendLockPlayerCameraView(entity, viewType, duration)
                    }
                }
            }
        }


    }


    val look = object : MultipleKetherParser("look") {

        @KetherEditor.Document("germengine look [durationTick: -1] [at objective:TargetContainer(sender)]")
        val lock = KetherHelper.combinedKetherParser {
            it.group(long().option().defaultsTo(-1), commandObjective()).apply(it) { duration, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendLockPlayerCameraRotate(entity, duration)
                    }
                }
            }
        }

        @KetherEditor.Document("germengine look unlock [at objective:TargetContainer(sender)]")
        val unlock = KetherHelper.combinedKetherParser {
            it.group(commandObjective()).apply(it) { objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendUnlockPlayerCameraRotate(entity)
                    }
                }
            }
        }

        @KetherEditor.Document("germengine look at <duration> [aim objective:TargetContainer(empty)] [at objective:TargetContainer(sender)]")
        val at = KetherHelper.combinedKetherParser {
            it.group(long(), commandObjectiveOrEmpty("aim"), commandObjective()).apply(it) { duration, aim, objective ->
                now {
                    val entity = objective.filterIsInstance<TargetBukkitEntity>().firstOrNull() ?: return@now
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val player = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendLockPlayerCameraFaceEntity(player, entity.instance.entityId, duration)
                    }
                }
            }
        }

    }


    val move = object : MultipleKetherParser("move") {

        @KetherEditor.Document("germengine move [durationTick: -1] [at objective:TargetContainer(sender)]")
        val lock = KetherHelper.combinedKetherParser {
            it.group(long().option().defaultsTo(-1), commandObjective()).apply(it) { duration, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendLockPlayerMove(entity, duration)
                    }
                }
            }
        }

        @KetherEditor.Document("germengine move unlock [at objective:TargetContainer(sender)]")
        val unlock = KetherHelper.combinedKetherParser {
            it.group(commandObjective()).apply(it) { objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendUnlockPlayerMove(entity)
                    }
                }
            }
        }

    }


    @KetherEditor.Document("germengine sound <name> [type: (master)] [volume: 1.0] [pitch: 1.0] [at objective:TargetContainer(sender)]")
    val sound = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            text().option().defaultsTo("master"),
            float().option().defaultsTo(1f),
            float().option().defaultsTo(1f),
            commandObjective()
        ).apply(it) { name, type, volume, pitch, objective ->
            now {
                val soundType = SoundType.valueOf(type.uppercase(Locale.getDefault()))
                objective.forEach {
                    if (it is TargetBukkitEntity && it.instance is Player) {
                        val player = it.instance
                        val x = player.location.x.cfloat
                        val y = player.location.y.cfloat
                        val z = player.location.z.cfloat
                        GermPacketAPI.playSound(player, name, soundType, x, y, z, 0, volume, pitch)
                    } else if (it is TargetLocation<*>) {
                        GermPacketAPI.playSound(it.getBukkitLocation(), name, soundType, 0, volume, pitch)
                    }
                }
            }
        }
    }


    @KetherEditor.Document("germengine cooldown <slot> <tick> [at objective:TargetContainer(sender)]")
    val cooldown = KetherHelper.combinedKetherParser {
        it.group(text(), int(), commandObjective()).apply(it) { slot, tick, objective ->
            now {
                val equipment = BukkitEquipment.valueOf(slot.uppercase(Locale.getDefault()))
                objective.filterIsInstance<TargetBukkitEntity>().forEach {
                    val player = it.instance as? Player ?: return@forEach
                    GermPacketAPI.setItemStackCooldown(player, equipment.getItem(player), tick)
                }
            }
        }
    }


    val animation = object : MultipleKetherParser() {

        @KetherEditor.Document("germengine animation stop <name> [at: TargetContainer(sender)]")
        val stop = KetherHelper.combinedKetherParser {
            it.group(text(), commandObjective()).apply(it) { name, objective ->
                now {
                    Bukkit.getOnlinePlayers().forEach { sender ->
                        objective.filterIsInstance<TargetBukkitEntity>().forEach {
                            if (it.instance is Player) {
                                GermPacketAPI.sendBendClear(sender, it.instance.entityId)
                            } else {
                                GermPacketAPI.stopModelAnimation(sender, it.instance.entityId, name)
                            }
                        }
                    }
                }
            }
        }


        @KetherEditor.Document("germengine animation send <name> [speed: (1.0)] [reverse: (false)] [at: TargetContainer(sender)]")
        val send = KetherHelper.combinedKetherParser {
            it.group(
                text(),
                float().option().defaultsTo(1f),
                bool().option().defaultsTo(false),
                commandObjective()
            ).apply(it) { name, speed, reverse, objective ->
                now {
                    val animDataDTO = AnimDataDTO(name, speed, reverse)
                    Bukkit.getOnlinePlayers().forEach { sender ->
                        objective.filterIsInstance<TargetBukkitEntity>().forEach {
                            val entityId = it.instance.entityId
                            if (it.instance is Player) {
                                GermPacketAPI.sendBendAction(sender, entityId, animDataDTO)
                            } else {
                                GermPacketAPI.sendModelAnimation(sender, entityId, animDataDTO)
                            }
                        }
                    }
                }
            }
        }
    }


}
