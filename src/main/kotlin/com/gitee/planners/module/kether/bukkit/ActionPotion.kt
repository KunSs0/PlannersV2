package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.commandObjectiveOrSender
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.function.warning
import taboolib.common5.Coerce
import taboolib.library.kether.QuestActionParser
import taboolib.library.xseries.XPotion
import taboolib.module.kether.combinationParser
import kotlin.jvm.optionals.getOrNull

@CombinationKetherParser.Used
object ActionPotion : MultipleKetherParser("potion") {

    // potion add <id> [level 1] [duration 20] [at objective:TargetContainer(sender)]
    val add = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            command("level", then = int()).option().defaultsTo(1),
            command("duration", then = int()).option().defaultsTo(20),
            commandObjectiveOrSender()
        ).apply(it) { id, level, duration, objective ->
            now {
                val xPotion = XPotion.of(id).getOrNull()
                if (xPotion == null) {
                    warning("Invalid potion effect ID: $id")
                    return@now
                }
                println("potion effect: $xPotion, level: $level, duration: $duration, objective: ${objective.size}")

                val potionEffect = PotionEffect(xPotion.get()!!, Coerce.toInteger(duration * 20L), level)
                objective.filterIsInstance<TargetBukkitEntity>().forEach { target ->
                    val entity = target.instance
                    if (entity is LivingEntity) {
                        entity.addPotionEffect(potionEffect)
                    }
                }
            }
        }
    }

}