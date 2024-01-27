package com.gitee.planners.api

import com.gitee.planners.api.common.registry.SingletonConfigurationRegistry
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.config.ImmutableKeyBinding
import taboolib.common.platform.Awake
import taboolib.library.configuration.ConfigurationSection

@Awake
object KeyBindingAPI : SingletonConfigurationRegistry<KeyBinding>("key-binding.yml") {

    override fun invokeInstance(config: ConfigurationSection): KeyBinding {
        return ImmutableKeyBinding(config)
    }

}
