package com.gitee.planners.module.kether.attribute

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.compat.attribute.AttributeDriver
import com.gitee.planners.module.kether.commandInt
import com.gitee.planners.module.kether.commandObjectiveOrSender
import taboolib.common.util.asList

@CombinationKetherParser.Used
object AttributeModifier : MultipleKetherParser("attribute", "attr") {

    /**
     * attribute add <id> [duration <tick>(-1)] <values([ 攻击力+10 攻击力 + 20 ])> [at <objective:TargetContainer(sender)>]
     * Example:
     *   attribute add "a0" [ 攻击力+1 防御力+2 ]
     *   attribute add "a0" duration 100 [ 攻击力+10 攻击力 + 20 ] at @sender
     * */
    val add = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            commandInt("duration", -1),
            any().defaultsTo(emptyList<String>()),
            commandObjectiveOrSender()
        ).apply(it) { id, duration, attr, objective ->
            val template = attr!!.asList().flatMap { it.split(",") }
            now {
                objective.filterIsInstance<TargetBukkitEntity>().forEach {
                    AttributeDriver.set(it, id, template, duration)
                }
            }
        }
    }

    // attribute remove <id> [at <objective:TargetContainer(sender)>]
    // attribute remove "a0" at @sender
    val remove = KetherHelper.combinedKetherParser {
        it.group(
            text(),
            commandObjectiveOrSender()
        ).apply(it) { id, objective ->
            now {
                objective.filterIsInstance<TargetBukkitEntity>().forEach {
                    AttributeDriver.remove(it, id)
                }
            }
        }
    }

}
