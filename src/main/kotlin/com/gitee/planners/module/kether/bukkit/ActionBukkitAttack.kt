package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.*
import org.bukkit.entity.Damageable
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.warning
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

//@KetherEditor.Document("attack <value: number> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
//@CombinationKetherParser.Used
//object ActionBukkitAttack : SimpleKetherParser("attack") {
//    override fun run(): QuestActionParser {
//        return combinationParser {
//            it.group(
//                actionDouble(),
//                commandObjectiveOrEmpty(),
//                commandObjectiveOrSender("source")
//            ).apply(it) { value, objective, source ->
//                val killer = source.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.instance as? LivingEntity
//                now {
//                    if (killer == null) {
//                        warning("Action attack source not correctly defined")
//                        return@now
//                    }
//                    objective.filterIsInstance<TargetBukkitEntity>().map { it.instance }.forEach { entity ->
//                        if (entity is Damageable) {
//                            entity.damage(value,killer)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
