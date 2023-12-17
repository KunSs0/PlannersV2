package com.gitee.planners.core.action.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.core.action.commandBool
import com.gitee.planners.core.action.commandObjective


@KetherEditor.Document("explosion <power> <fire:bool(false)> <break:bool(false)> [at objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
fun actionExplosion() = KetherHelper.combinedKetherParser("explosion") {
    it.group(float(),commandBool("fire"),commandBool("break"),commandObjective(LeastType.ORIGIN)).apply(it) { power, isFire, isBreak, objective ->
        now {
            objective.filterIsInstance<TargetLocation<*>>().forEach {
                val world = it.getBukkitLocation().world ?: return@forEach
                world.createExplosion(it.getBukkitLocation(),power,isFire,isBreak)
            }
        }
    }
}
