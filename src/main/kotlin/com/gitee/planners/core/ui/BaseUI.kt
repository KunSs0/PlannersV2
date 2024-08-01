package com.gitee.planners.core.ui

import com.gitee.planners.core.ui.KeyBindingsEditorUI.title
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.module.ui.ClickEvent
import taboolib.module.ui.type.Linked
import taboolib.module.ui.type.PageableChest
import taboolib.module.ui.type.impl.ChestImpl
import java.util.function.Supplier

interface BaseUI {

    fun openTo(player: Player) {
        player.openInventory(this.display(player).build())
    }

    fun display(player: Player): Display


    interface Display {

        fun build(): Inventory

    }

    class DisplayLinked<T>(title: String) : Linked<T>(title), Display

    class Chest(title: String) : ChestImpl(title), Display


    companion object {

        fun createBaseUI(block: () -> Display): BaseUI {

            return object : BaseUI {
                override fun display(player: Player): Display {
                    return block()
                }
            }
        }

        fun <T> linked(title: String, block: DisplayLinked<T>.() -> Unit): DisplayLinked<T> {
            return DisplayLinked<T>(title).also(block)
        }

        fun chest(title: String, block: Chest.() -> Unit): Chest {
            return Chest(title).also(block)
        }

        fun chest(ui: AutomationBaseUI, block: Chest.() -> Unit) : Chest {
            return chest(ui.title) {
                rows(ui.rows)
                block(this)
            }
        }

        fun Chest.setIcon(icon: AutomationBaseUI.Icon,block: ClickEvent.() -> Unit) {
            setIcon(icon,icon.icon,block)
        }

        fun Chest.setIcon(icon: AutomationBaseUI.Icon,build: ItemStack,block: ClickEvent.() -> Unit) {
            icon.slots.forEach {
                this.set(it,build,block)
            }
        }
    }

}
