package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.commandBool
import com.gitee.planners.module.kether.commandObjective


@KetherEditor.Document("freeze <tick> [at objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
fun actionFreeze() = KetherHelper.combinedKetherParser("freeze") {
    it.group(int(), commandObjective(type = LeastType.SENDER)).apply(it) { tick, objective ->
        now {
            objective.filterIsInstance<TargetBukkitEntity>().forEach {
                it.instance.freezeTicks = tick
            }
        }
    }

}

@KetherEditor.Document("fire <tick> [at objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
fun actionFire() = KetherHelper.combinedKetherParser("fire") {
    it.group(int(), commandObjective(type = LeastType.SENDER)).apply(it) { tick, objective ->
        now {
            objective.filterIsInstance<TargetBukkitEntity>().forEach {
                it.instance.fireTicks = tick
            }
        }
    }
}

@KetherEditor.Document("explosion <power> <fire:bool(false)> <break:bool(false)> [at objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
fun actionExplosion() = KetherHelper.combinedKetherParser("explosion") {
    it.group(float(), commandBool("fire"), commandBool("break"), commandObjective(type = LeastType.ORIGIN))
        .apply(it) { power, isFire, isBreak, objective ->
            now {
                objective.filterIsInstance<TargetLocation<*>>().forEach {
                    val world = it.getBukkitLocation().world ?: return@forEach
                    val location = it.getBukkitLocation()
                    world.createExplosion(location.x, location.y, location.z, power, isFire, isBreak)
                }
            }
        }
}
