package com.gitee.planners

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration

object Planners : Plugin() {

    @Config
    lateinit var config: Configuration
        private set


    override fun onEnable() {
        info("Hello TabooLib")
    }

}
