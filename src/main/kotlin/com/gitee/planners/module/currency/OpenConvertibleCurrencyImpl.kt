package com.gitee.planners.module.currency

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.common.script.KetherScript.Companion.PARSER_DOUBLE
import com.gitee.planners.api.common.script.KetherScript.Companion.getNow
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.common.script.SingletonKetherScript
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.context.CompiledScriptContext
import com.gitee.planners.module.kether.context.Context
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapValue
import java.util.concurrent.CompletableFuture
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

        return actions["hook"]!!.getNow(player, PARSER_DOUBLE)
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


    class SimpleAction(experience: String) : SingletonKetherScript(experience), ComplexCompiledScript {

        override val id: String = experience

        override val async: Boolean = false

        override fun namespaces(): List<String> {
            return listOf(KetherHelper.NAMESPACE_COMMON)
        }

        override fun platform(): ComplexScriptPlatform {
            return Currencies
        }

        override fun source(): String {
            return action
        }

        fun runNow(player: Player, vararg args: Pair<String, Any?>) {
            val ctx = newCtx(player)

            this.run(ctx.optionsBuilder {
                it.vars(*args)
            })
        }

        inline fun <reified T> getNow(sender: Player, parser: Function<Any?, T>): T {
            return getNow(newCtx(sender).optionsBuilder(), parser)
        }

        /**
         * 创建一个新的上下文
         *
         * @param player 玩家
         * @param action 动作
         */
        fun newCtx(player: Player): CompiledScriptContext {
            return CompiledScriptContext(adaptTarget(player), this)
        }

    }

}
