package com.gitee.planners.core.config

import com.gitee.planners.api.job.KeyBinding
import taboolib.library.configuration.ConfigurationSection

class ImmutableKeyBinding(val config: ConfigurationSection) : KeyBinding {

    override val id = config.name

    override val name = config.getString("name", id)!!

}
