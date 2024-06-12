package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.module.kether.getTargetContainer

/**
 * 打乱选择器容器顺序
 *
 * @shuffle
 */
object Shuffle : AbstractSelector("shuffle", "") {
    override fun select(): SimpleKetherParser {
        return KetherHelper.simpleKetherNow {
            getTargetContainer().shuffle()
        }
    }
}
