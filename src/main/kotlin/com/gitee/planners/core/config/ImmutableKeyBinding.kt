package com.gitee.planners.core.config

import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.skill.binding.Combined
import taboolib.library.configuration.ConfigurationSection

class ImmutableKeyBinding(config: ConfigurationSection) : KeyBinding, Combined(config) {

    override val priority = config.getDouble("1.0")

}
