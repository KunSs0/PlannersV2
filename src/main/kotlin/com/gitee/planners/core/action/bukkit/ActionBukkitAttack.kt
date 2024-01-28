package com.gitee.planners.core.action.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.core.action.*
import org.bukkit.entity.Damageable
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.warning
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

@KetherEditor.Document("attack <value: number> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
object ActionBukkitAttack : SimpleKetherParser("attack") {
    override fun run(): QuestActionParser {
        return combinationParser {
            it.group(
                actionDouble(),
                commandObjectiveOrEmpty(),
                commandObjectiveOrSender("source")
            ).apply(it) { value, objective, source ->
                val killer = source.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.getInstance() as? LivingEntity
                now {
                    if (killer == null) {
                        warning("Action attack source not correctly defined")
                        return@now
                    }
                    objective.filterIsInstance<TargetBukkitEntity>().map { it.getInstance() }.forEach { entity ->
                        if (entity is Damageable) {
                            entity.damage(value,killer)
                        }
                    }
                }
            }
        }
    }
}
