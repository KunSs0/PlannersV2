package com.gitee.planners.core.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common5.cint
import taboolib.module.ui.ClickEvent

abstract class SingletonChoiceUI<T>(name: String) : AutomationBaseUI(name) {

    @Option("__option__.slots")
    val slots = simpleConfigNodeTo<List<Any>, List<Int>> {
        map { it.cint }
    }


    abstract fun onGenerate(player: Player, element: T, index: Int, slot: Int): ItemStack

    abstract fun onClick(event: ClickEvent, element: T)

    abstract fun getElements(player: Player): Collection<T>

    open fun onClose(player: Player) {}

    override fun display(player: Player): BaseUI.Display {

        return BaseUI.linked<T>(title) {
            rows(this@SingletonChoiceUI.rows)
            slots(this@SingletonChoiceUI.slots.get())
            elements { this@SingletonChoiceUI.getElements(player).toList() }

            onGenerate { player, element, index, slot ->
                this@SingletonChoiceUI.onGenerate(player, element, index, slot)
            }

            onClick { event, element ->
                this@SingletonChoiceUI.onClick(event, element)
            }

            onClose {
                this@SingletonChoiceUI.onClose(it.player as Player)
            }

        }
    }

}
