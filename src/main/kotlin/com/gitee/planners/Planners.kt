package com.gitee.planners

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

object ExampleProject : Plugin() {

    override fun onEnable() {
        info("Hello TabooLib")
    }

}