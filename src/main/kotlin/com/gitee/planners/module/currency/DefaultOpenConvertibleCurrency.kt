package com.gitee.planners.module.currency

import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.common.script.SingletonKetherScript
import org.bukkit.entity.Player
import taboolib.common5.cdouble
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapValue

class DefaultOpenConvertibleCurrency(val root: ConfigurationSection) : OpenConvertibleCurrency {

    override val id = root.name

    override val name = root.getString("name")!!

    val actions = root.mapValue {
        SingletonKetherScript(it.toString())
    }

    override fun get(player: Player): Double {
        return actions["get"]!!.get(KetherScriptOptions.common(player)) {
            it.cdouble
        }
    }

    override fun give(player: Player, amount: Double): Boolean {
        actions["give"]!!.run(KetherScriptOptions.create {
            this.sender(player)
            this.set("arg", amount)
        })
        return true
    }

    override fun take(player: Player, amount: Double): Boolean {
        if (this.get(player) < amount) {
            return false
        }

        actions["take"]!!.run(KetherScriptOptions.create {
            this.sender(player)
            this.set("arg", amount)
        })
        return true
    }

    override fun set(player: Player, amount: Double) {
        actions["set"]!!.run(KetherScriptOptions.create {
            this.sender(player)
            this.set("arg", amount)
        })
    }

    override fun has(player: Player, amount: Double): Boolean {
        return this.get(player) >= amount
    }

}
