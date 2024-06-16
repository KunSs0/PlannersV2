package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.KeyBinding
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.util.replaceWithOrder
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XItemStack
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.buildItem
import java.util.function.Consumer

/**
 * 线程不安全
 */
object KeyBindingsEditorUI : SingletonChoiceUI<KeyBinding>("key-bindings-editor.yml") {

    private val callback = mutableMapOf<Player, Consumer<KeyBinding>>()

    @Option("__option__.fill-icon")
    val iconFill = simpleConfigNodeTo<ConfigurationSection, ItemStack> {
        XItemStack.deserialize(this)
    }

    override fun onGenerate(player: Player, element: KeyBinding, index: Int, slot: Int): ItemStack {
        val profile = player.plannersProfile
        val skill = profile.getRegisteredSkillOrNull(element)
        return if (skill == null) {
            buildItem(iconFill.get()) {
                name = name?.replaceWithOrder(element.name)
            }
        } else {
            KeyBindingAPI.createIconFormatter(player, skill).build()
        }
    }

    override fun onClick(event: ClickEvent, element: KeyBinding) {
        callback[event.clicker]?.accept(element)
    }

    override fun getElements(player: Player): Collection<KeyBinding> {
        return Registries.KEYBINDING.values()
    }

    override fun onClose(player: Player) {
        this.callback.remove(player)
    }

    /**
     * 非回调式选中
     */
    fun choice(player: Player) {
        this.callback.remove(player)
        openTo(player)
    }

    fun choice(player: Player, func: (binding: KeyBinding) -> Unit) {
        this.callback[player] = Consumer {
            func(it)
        }
        openTo(player)
    }

}
