package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.commandEnum
import com.gitee.planners.module.kether.getTargetContainer
import taboolib.module.kether.combinationParser

/**
 * 转换容器内所有非location目标，如果遇到entity目标，会根据rule转换规则取眼睛位置或者脚下位置,不填入rule默认为脚下位置
 * convert-to-location [rule: text(default)>]
 * * ct-location ...
 * ct-loc ...
 */
object ConvertToLocation: Selector {
    override fun namespace(): Array<String> {
        return arrayOf("convert-to-location","ct-location","ct-loc")
    }

    override fun action() = KetherHelper.combinedKetherParser {
        it.group(commandEnum("rule",MatchRule.DEFAULT)).apply(it) { match ->
            now {
                this.getTargetContainer().modified {
                    if (it is TargetEntity<*>) {
                        when (match) {
                            MatchRule.DEFAULT -> {
                                it.getBukkitLocation().adaptTarget()
                            }
                            MatchRule.EYE -> {
                                it.getBukkitEyeLocation().adaptTarget()
                            }
                            else -> error("Unknown match-rule type for $match")
                        }
                    }  else {
                        it
                    }
                }
            }
        }
    }

    private enum class MatchRule {

        DEFAULT,EYE

    }

}
