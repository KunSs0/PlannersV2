package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.commandDouble
import com.gitee.planners.module.kether.commandInt
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.module.kether.actionNow

/**
 * 获取沿视角方向的目标方块
 * lookBlock [distance int(5)]
 */
object LookTargetBlock : AbstractSelector("look-target-block", "look-block", "lookBlock") {

    override fun select() = KetherHelper.combinedKetherParser {
        it.group(commandInt("distance", 5)).apply(it) { distance ->
            now {
                val target = getEnvironmentContext().origin
                if (target !is TargetBukkitEntity) {
                    error("origin is not a entity")
                }
                val entity = target.instance as LivingEntity
                // 注册到容器内
                getTargetContainer().add(adaptTarget(entity.getTargetBlock(null, distance!!)))
            }
        }
    }

}
