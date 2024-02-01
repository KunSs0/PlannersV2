package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.module.kether.commandEnum
import com.gitee.planners.module.kether.getTargetContainer

object Name : AbstractSelector("name") {

    override fun select() = KetherHelper.combinedKetherParser {
        it.group(text(), commandEnum("rule",Type.FUZZY)).apply(it) { name,rule ->
            now {
                when (rule) {

                    Type.FUZZY -> {
                        getTargetContainer().removeIf { (it as? Target.Named)?.getName()?.contains(name) == false }
                    }

                    Type.STRICT -> {
                        getTargetContainer().removeIf { (it as? Target.Named)?.getName() != name }
                    }

                    else -> error("Unexpected rule type $rule")
                }
            }
        }
    }


    private enum class Type {

        FUZZY,STRICT

    }

}
