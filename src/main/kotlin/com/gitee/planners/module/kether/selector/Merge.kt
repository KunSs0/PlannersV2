package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.module.kether.getTargetContainer
import com.gitee.planners.module.kether.objective

/**
 * merge [objective]
 */
object Merge : AbstractSelector("merge") {
    override fun select() = KetherHelper.combinedKetherParser {
        it.group(anyAsList()).apply(it) { objectives ->
            now {
                objectives.forEach { o ->
                    when (o) {
                        null -> {
                            error("Objective is missing!")
                        }
                        is Target<*> -> {
                            getTargetContainer().add(o)
                        }

                        is TargetContainer -> {
                            getTargetContainer().addAll(o)
                        }

                        else -> {
                            error("Objective $o is not a target and container.")
                        }
                    }
                }
            }
        }
    }

}
