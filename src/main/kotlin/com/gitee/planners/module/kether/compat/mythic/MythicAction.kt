package com.gitee.planners.module.kether.compat.mythic

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.commandObjectiveOrEmpty
import com.gitee.planners.module.kether.commandObjectiveOrSender
import com.gitee.planners.module.kether.ctx
import ink.ptms.um.Mythic
import io.lumine.mythic.api.mobs.MythicMob
import io.lumine.mythic.bukkit.BukkitAPIHelper
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.common5.cint
import taboolib.module.kether.KetherShell

@CombinationKetherParser.Used
object MythicAction : MultipleKetherParser("mythic","mm") {

    val version: Int by unsafeLazy {
        val plugin = Bukkit.getPluginManager().getPlugin("MythicMobs")
        if (plugin == null) {
            -1
        } else {
            info("Found MythicMobs ${plugin.description.version}")
            plugin.description.version.split(".")[0].cint
        }
    }

    private val isEnable: Boolean
        get() = version != -1

    val mob = object : MultipleKetherParser("mob") {

        // mythic mob threat <amount> [at objective]
        val threat = KetherHelper.combinedKetherParser("threat-table","tt") {
            it.group(double(), commandObjectiveOrEmpty()).apply(it) { amount, objective ->
                now {
                    if (!isEnable) {
                        warning("MythicMobs is not installed.")
                        return@now
                    }
                    val sender = ctx().sender.instance as? LivingEntity
                    if (sender == null) {
                        warning("Sender is not a living entity or not found.")
                        return@now
                    }
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance
                        if (version == 4) {
                            MythicMobs.inst().apiHelper.addThreat(entity,sender,amount)
                        } else if (version == 5) {
                            MythicBukkit.inst().apiHelper.addThreat(entity,sender,amount)
                        }
                    }
                }
            }
        }

        // mythic mob signal <signal> [at objective]
        val signal = KetherHelper.combinedKetherParser {
            it.group(any(), commandObjectiveOrEmpty()).apply(it) { signal, objective ->
                now {
                    if (!isEnable) {
                        warning("MythicMobs is not installed.")
                        return@now
                    }
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance
                        if (version == 4) {
                            val activeMob = MythicMobs.inst().mobManager.getActiveMob(entity.uniqueId)
                            if (activeMob.isPresent) {
                                activeMob.get().signalMob(
                                    io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter.adapt(entity),
                                    signal.toString()
                                )
                            }
                        } else if (version == 5) {
                            val activeMob = MythicBukkit.inst().mobManager.getActiveMob(entity.uniqueId)
                            if (activeMob.isPresent) {
                                activeMob.get().signalMob(BukkitAdapter.adapt(entity), signal.toString())
                            }
                        }
                    }
                }
            }
        }

    }

    @KetherEditor.Document("mythic cast <id> [at <objective:TargetContainer(sender)>]")
    val cast = KetherHelper.combinedKetherParser {
        it.group(text(), commandObjectiveOrSender()).apply(it) { id, objective ->
            now {
                if (!isEnable) {
                    warning("MythicMobs is not installed.")
                    return@now
                }
                objective.filterIsInstance<TargetBukkitEntity>().forEach {
                    val entity = it.instance

                    Mythic.API.castSkill(entity, id)
                }
            }
        }
    }


}
