package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.commandObjectiveOrSender
import org.bukkit.entity.LivingEntity

@CombinationKetherParser.Used
object ActionHealth : MultipleKetherParser("health") {

    // health add <amount> [at objective:TargetContainer(sender)]
    val add = KetherHelper.combinedKetherParser {
        it.group(
            double(),
            commandObjectiveOrSender()
        ).apply(it) { amount, objective ->
            now {
                objective.filterIsInstance<TargetBukkitEntity>().forEach { target ->
                    val entity = target.instance
                    if (entity is LivingEntity) {
                        entity.health = (entity.health + amount).coerceIn(0.0, entity.maxHealth)
                    }
                }
            }
        }
    }

    // health set <amount> [at objective:TargetContainer(sender)]
    val set = KetherHelper.combinedKetherParser {
        it.group(
            double(),
            commandObjectiveOrSender()
        ).apply(it) { amount, objective ->
            now {
                objective.filterIsInstance<TargetBukkitEntity>().forEach { target ->
                    val entity = target.instance
                    if (entity is LivingEntity) {
                        entity.health = amount.coerceIn(0.0, entity.maxHealth)
                    }
                }
            }
        }
    }

    // health take <amount> [at objective:TargetContainer(sender)]
    val take = KetherHelper.combinedKetherParser {
        it.group(
            double(),
            commandObjectiveOrSender()
        ).apply(it) { amount, objective ->
            now {
                objective.filterIsInstance<TargetBukkitEntity>().forEach { target ->
                    val entity = target.instance
                    if (entity is LivingEntity) {
                        entity.health = (entity.health - amount).coerceIn(0.0, entity.maxHealth)
                    }
                }
            }
        }
    }

}