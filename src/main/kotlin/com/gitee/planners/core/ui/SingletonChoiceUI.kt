package com.gitee.planners.core.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.common5.cint
import taboolib.module.configuration.Configuration
import taboolib.module.ui.ClickEvent

abstract class SingletonChoiceUI<T>(name: String) : AutomationBaseUI(name) {

    @Option("__option__.slots")
    val slots = simpleConfigNodeTo<List<Any>, List<Int>> {
        map { it.cint }
    }

    @Option("*")
    val decorateIcon = decorateIcon()

    abstract fun onGenerate(player: Player, element: T, index: Int, slot: Int): ItemStack

    abstract fun onClick(event: ClickEvent, element: T)

    abstract fun getElements(player: Player): List<T>

    override fun display(player: Player): BaseUI.Display {

        return BaseUI.linked<T>(title) {
            rows(this@SingletonChoiceUI.rows)
            slots(this@SingletonChoiceUI.slots.get())
            elements { this@SingletonChoiceUI.getElements(player) }

            onGenerate { player, element, index, slot ->
                this@SingletonChoiceUI.onGenerate(player, element, index, slot)
            }

            onClick { event, element ->
                this@SingletonChoiceUI.onClick(event, element)
            }

        }
    }

    override fun openTo(player: Player) {
        player.openInventory(decorateTo(decorateIcon.get(), this.display(player).build()))
    }

    fun decorateIcon() = simpleConfigNodeTo<Configuration, List<DecorateIcon>> {
        this.getKeys(false).filter { it != "__option__" }.map {
            DecorateIcon(this.getConfigurationSection(it)!!)
        }
    }

    fun decorateTo(icons: List<DecorateIcon>, inventory: Inventory): Inventory {
        icons.forEach { icon ->
            icon.slots.forEach {
                inventory.setItem(it, icon.icon)
            }
        }
        return inventory
    }
}
