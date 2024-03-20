package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.module.kether.getTargetContainer
import com.gitee.planners.module.kether.objective

/**
 * merge [objective]
 */
object Merge : AbstractSelector("merge") {
    override fun select() = KetherHelper.combinedKetherParser {
        it.group(objective()).apply(it) {
            now {
                getTargetContainer() += it
            }
        }
    }

}
