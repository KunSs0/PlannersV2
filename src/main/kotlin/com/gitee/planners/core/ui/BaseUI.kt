package com.gitee.planners.core.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.module.ui.type.Linked
import java.util.function.Supplier

interface BaseUI {

    fun openTo(player: Player) {
        player.openInventory(this.display(player).build())
    }

    fun display(player: Player): Display

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Option(val node: String)

    interface Display {

        fun build(): Inventory

    }

    class DisplayLinked<T>(title: String) : Linked<T>(title), Display


    companion object {

        fun <T> linked(title: String, block: DisplayLinked<T>.() -> Unit): DisplayLinked<T> {
            return DisplayLinked<T>(title).also(block)
        }

    }

}
