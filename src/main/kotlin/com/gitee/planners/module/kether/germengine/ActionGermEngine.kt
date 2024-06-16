package com.gitee.planners.module.kether.germengine

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.RootType
import com.germ.germplugin.api.SoundType
import com.germ.germplugin.api.ViewType
import com.germ.germplugin.api.bean.AnimDataDTO
import com.germ.germplugin.api.dynamic.animation.IAnimatable
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.*
import com.gitee.planners.module.kether.common.ActionContext.sender
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.common5.cfloat
import taboolib.platform.util.onlinePlayers
import taboolib.type.BukkitEquipment
import java.util.*

@CombinationKetherParser.Used
object ActionGermEngine : MultipleKetherParser("germengine") {

    val effect = object : MultipleKetherParser("effect") {


        @KetherEditor.Document("germengine effect send <name> [apply ([ProxyGermAnimation])] [at objective:TargetContainer(sender)]")
        val send = KetherHelper.combinedKetherParser {
            it.group(
                text(),
                command("apply", then = anyAsList()).optional(),
                commandObjective()
            ).apply(it) { namespace, animations, objective ->
                now {
                    val data = GermEffectManager.get(namespace, RootType.EFFECT)
                    animations.orElse(mutableListOf()).filterIsInstance<ProxyGermAnimation>().forEach { animation ->
                        (data as? IAnimatable<*>)?.addAnimation(animation.create())
                    }
                    objective.forEach { target ->
                        info("send effect $target $data")
                        if (target is TargetBukkitEntity) {
                            onlinePlayers.forEach { data.spawnToEntity(it, target.instance) }
                        } else if (target is TargetLocation<*>) {
                            onlinePlayers.forEach { data.spawnToLocation(it, target.getBukkitLocation()) }
                        }
                    }
                }
            }
        }

        @KetherEditor.Document("germengine effect <name> [apply ([ProxyGermAnimation])] [at objective:TargetContainer(sender)]")
        val main = send

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

        @KetherEditor.Document("germengine view [durationTick: -1] [type: (first.person)] [at objective:TargetContainer(sender)]")
        val lock = KetherHelper.combinedKetherParser {
            it.group(
                commandLong("durationTick", -1L),
                commandEnum<ViewType>("1", ViewType.FIRST_PERSON),
                commandObjective()
            ).apply(it) { duration, type, objective ->
                now {

                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as? Player ?: return@forEach
                        GermPacketAPI.sendLockPlayerCameraView(entity, type, duration)
                    }
                }
            }
        }


    }


    val look = object : MultipleKetherParser("look") {

        @KetherEditor.Document("germengine look [durationTick: -1] [at objective:TargetContainer(sender)]")
        val lock = KetherHelper.combinedKetherParser {
            it.group(commandLong("duration", -1), commandObjective()).apply(it) { duration, objective ->
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
            it.group(commandLong("duration", -1), commandObjective()).apply(it) { duration, objective ->
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

    val test = KetherHelper.combinedKetherParser {
        it.group(text()).apply(it) { data ->
            now { "test:$data" }
        }
    }

    @KetherEditor.Document("germengine sound <name> [type: (master)] [volume: 1.0] [pitch: 1.0] [at objective:TargetContainer(sender)]")
    val sound = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            commandEnum<SoundType>("type", SoundType.MASTER),
            commandFloat("volume", 1f),
            commandFloat("pitch", 1f),
            commandObjectiveOrSender()
        ).apply(it) { name, type, volume, pitch, objective ->
            now {
                info("play sound $name $type $volume $pitch")
                objective.forEach {
                    if (it is TargetBukkitEntity && it.instance is Player) {
                        val player = it.instance
                        val x = player.location.x.cfloat
                        val y = player.location.y.cfloat
                        val z = player.location.z.cfloat
                        GermPacketAPI.playSound(player, name, type, x, y, z, 0, volume, pitch)
                    } else if (it is TargetLocation<*>) {
                        GermPacketAPI.playSound(it.getBukkitLocation(), name, type, 0, volume, pitch)
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
                commandFloat("speed", 1f),
                commandBool("reverse", false),
                commandObjectiveOrSender()
            ).apply(it) { name, speed, reverse, objective ->
                now {
                    val animDataDTO = AnimDataDTO(name, speed, reverse)
                    Bukkit.getOnlinePlayers().forEach { sender ->
                        objective.filterIsInstance<TargetBukkitEntity>().forEach {
                            val entityId = it.instance.entityId
                            if (it.instance is Player) {
                                info("send bend action $sender $entityId $animDataDTO")
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
