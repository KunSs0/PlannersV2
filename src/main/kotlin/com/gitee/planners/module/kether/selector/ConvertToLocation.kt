package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.TargetBukkitLocation
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.actionVector
import com.gitee.planners.module.kether.commandEnum
import com.gitee.planners.module.kether.getTargetContainer
import com.gitee.planners.util.math.asVector
import taboolib.common.util.Vector
import taboolib.module.kether.combinationParser

/**
 * 转换容器内所有非location目标，如果遇到entity目标，会根据rule转换规则取眼睛位置或者脚下位置,不填入rule默认为脚下位置
 * convert-to-location [rule: text(default)>] [offset: vector(0 0 0)]
 * * ct-location ...
 * ct-loc ...
 */
object ConvertToLocation : AbstractSelector("convert-to-location", "ct-location", "ct-loc") {

    override fun select() = KetherHelper.combinedKetherParser {
        it.group(
            commandEnum("rule", Type.DEFAULT),
            command("offset", then = actionVector()).option().defaultsTo(Vector())
        ).apply(it) { match, offset ->
            val offsetX = offset.x
            val offsetY = offset.y
            val offsetZ = offset.z

            now {
                this.getTargetContainer().modified {
                    val target = if (it is TargetEntity<*>) {
                        when (match) {
                            Type.DEFAULT -> {
                                (it.getBukkitLocation()).adaptTarget()
                            }

                            Type.EYE -> {
                                it.getBukkitEyeLocation().adaptTarget()
                            }

                            else -> error("Unknown match-rule type for $match")
                        }
                    } else {
                        it
                    }
                    (target as TargetBukkitLocation).add(offsetX, offsetY, offsetZ)
                    target
                }
            }
        }
    }

    private enum class Type {

        DEFAULT, EYE

    }

}
