package com.gitee.planners.module.currency

import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.common5.cdouble
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapValue
import java.util.function.Function

class OpenConvertibleCurrencyImpl(val root: ConfigurationSection) : OpenConvertibleCurrency {

    override val id = root.name

    override val name = root.getString("name")!!

    val actions = root.mapValue("action") {
        SimpleAction(it.toString())
    }

    override fun get(player: Player): Double {
        if (!actions.containsKey("hook")) {
            warning("Currency $id does not have hook action.")
            return 0.0
        }

        return actions["hook"]!!.getNow(player, Function { it.cdouble })
    }

    override fun give(player: Player, amount: Double): Boolean {
        if (!actions.containsKey("deposit")) {
            warning("Currency $id does not have deposit action.")
            return false
        }
        actions["deposit"]!!.runNow(player, "arg" to amount)
        return true
    }

    override fun take(player: Player, amount: Double): Boolean {
        if (!actions.containsKey("withdraw")) {
            warning("Currency $id does not have withdraw action.")
            return false
        }
        if (this.get(player) < amount) {
            return false
        }
        actions["withdraw"]!!.runNow(player, "arg" to amount)
        return true
    }

    override fun set(player: Player, amount: Double) {
        if (!actions.containsKey("set")) {
            warning("Currency $id does not have set action.")
            return
        }

        actions["set"]!!.runNow(player, "arg" to amount)
    }

    override fun has(player: Player, amount: Double): Boolean {
        return this.get(player) >= amount
    }


    class SimpleAction(action: String) : SingletonFluxonScript(action) {

        fun runNow(player: Player, vararg args: Pair<String, Any?>) {
            val options = FluxonScriptOptions.common(player)
            args.forEach { (k, v) -> options.set(k, v) }
            this.run(options)
        }

        inline fun <reified T> getNow(sender: Player, parser: Function<Any?, T>): T {
            val options = FluxonScriptOptions.common(sender)
            return parser.apply(this.eval(options))
        }

    }

}
