package com.gitee.planners.api

import com.gitee.planners.api.common.registry.Registry
import com.gitee.planners.api.common.registry.SingletonSortableConfigurationRegistry
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.config.ImmutableKeyBinding
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.module.skill.DynamicSkillIcon
import com.gitee.planners.module.skill.IconFormatter
import org.bukkit.entity.Player
import taboolib.common.platform.Awake
import taboolib.library.configuration.ConfigurationSection

@Registry.Load
object KeyBindingAPI : SingletonSortableConfigurationRegistry<KeyBinding>("key-binding.yml") {

    override fun invokeInstance(config: ConfigurationSection): KeyBinding {
        println("register key binding ${config.name}")
        return ImmutableKeyBinding(config)
    }

    fun createIconFormatter(player: Player, skill: PlayerSkill): IconFormatter {
        return DynamicSkillIcon(player.adaptTarget(), skill.immutable, skill.level)
    }

}
