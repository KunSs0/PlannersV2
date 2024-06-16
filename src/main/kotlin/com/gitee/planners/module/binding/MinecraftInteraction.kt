package com.gitee.planners.module.binding

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.util.configNodeTo
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.xseries.XItemStack
import taboolib.module.configuration.ConfigNode
import taboolib.platform.util.onlinePlayers

object MinecraftInteraction {

    @ConfigNode("settings.minecraft.interaction-action.enable")
    val isEnable = false

    @ConfigNode("settings.minecraft.interaction-action.empty-skill")
    val emptySkill = configNodeTo {
        XItemStack.deserialize(this)
    }

    fun clearInventory(player: Player) {
        Registries.KEYBINDING.values().forEach { binding ->
            val indexOf = Registries.KEYBINDING.values().indexOf(binding)
            player.inventory.setItem(indexOf, null)
        }
    }

    fun updateInventory(player: Player) = updateInventory(player.plannersProfile)

    fun updateInventory(profile: PlayerProfile) = Registries.KEYBINDING.values().forEach { binding ->
        updateBinding(profile,binding)
    }

    fun updateBinding(profile: PlayerProfile,binding: KeyBinding) {
        val skill = profile.getRegisteredSkillOrNull(binding)
        if (skill != null) {
            val formatter = KeyBindingAPI.createIconFormatter(profile.onlinePlayer, skill)
            updateBinding(profile.onlinePlayer,binding,formatter.build())
        }
    }

    fun updateBinding(player: Player, binding: KeyBinding,itemStack: ItemStack) {
        val indexOf = Registries.KEYBINDING.values().indexOf(binding)
        player.inventory.setItem(indexOf, itemStack)
    }

    fun updateInventory(profile: PlayerProfile, skill: PlayerSkill) {
        val binding = skill.binding ?: return
        val player = profile.onlinePlayer
        val formatter = KeyBindingAPI.createIconFormatter(player, skill)
        updateBinding(profile.onlinePlayer,binding,formatter.build())
    }

    fun execute(func: () -> Unit) {
        if (isEnable) func()
    }

    @SubscribeEvent
    fun e(e: PlayerSkillEvent.LevelChange) {
        execute {
            updateInventory(e.profile, e.skill)
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Pre) {
        execute {
            onlinePlayers.forEach { clearInventory(it) }
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Post) {
        execute {
            onlinePlayers.forEach { updateInventory(it) }
        }
    }


}
