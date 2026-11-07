package com.gitee.planners.module.action

import com.gitee.planners.api.Registries
import com.gitee.planners.module.action.query.QueryScope

/**
 * 货币动作与条件。
 *
 * 直接调用 `module/currency` 下已加载的货币实现，不重复实现经济逻辑。
 * 读取余额走条件 `currency`，避免动作产生不可见的返回值。
 */
object PlannersCurrencyActions {

    /**
     * 货币增减与设置。
     *
     * 参数 `op` 取 `give`、`take`、`set`。
     */
    object Currency : Action {

        override val id = "currency"

        override val aliases = listOf("currency")

        override fun validate(arguments: ActionArguments) {
            arguments.require("id")
            arguments.require("amount")
            val operation = arguments.literalText("op") ?: "give"
            if (!isSupported(operation)) {
                throw IllegalArgumentException(
                    "${arguments.path} op must be give, take or set.",
                )
            }
            arguments.rejectUnknown("id", "op", "amount")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val currencyId = arguments.requireText(scope, "id")
            val currency = resolve(currencyId, arguments.path)
            val amount = arguments.requireNumber(scope, "amount")
            val operation = arguments.text(scope, "op") ?: "give"
            for (target in targets) {
                val player = target.bukkitPlayer()
                if (player == null) {
                    continue
                }
                if (operation.equals("take", true)) {
                    currency.take(player, amount)
                    continue
                }
                if (operation.equals("set", true)) {
                    currency.set(player, amount)
                    continue
                }
                currency.give(player, amount)
            }
        }
    }

    /**
     * 货币余额区间判断。
     */
    object CurrencyCondition : ActionCondition {

        override val id = "currency-check"

        override val aliases = listOf("currency-check")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("id")
            if (!arguments.contains("min") && !arguments.contains("max")) {
                throw IllegalArgumentException("${arguments.path} requires 'min' or 'max'.")
            }
            arguments.rejectUnknown("id", "min", "max")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            val currencyId = arguments.requireText(scope, "id")
            val currency = resolve(currencyId, arguments.path)
            val player = scope.sender
            if (player !is org.bukkit.entity.Player) {
                return false
            }
            val balance = currency.get(player)
            val minimum = arguments.number(scope, "min")
            if (minimum != null && balance < minimum) {
                return false
            }
            val maximum = arguments.number(scope, "max")
            if (maximum != null && balance > maximum) {
                return false
            }
            return true
        }
    }

    /**
     * 按 ID 取货币实现。
     *
     * @param id 货币 ID。
     * @param path 配置路径，用于错误信息。
     * @return 货币实现。
     * @throws IllegalArgumentException 货币未注册时抛出。
     */
    private fun resolve(id: String, path: String): com.gitee.planners.module.currency.OpenConvertibleCurrency {
        val currency = Registries.CURRENCY.getOrNull(id)
        if (currency == null) {
            throw IllegalArgumentException("$path references unknown currency '$id'.")
        }
        return currency
    }

    /** 判断操作名是否受支持。 */
    private fun isSupported(operation: String): Boolean {
        return operation.equals("give", true) ||
            operation.equals("take", true) ||
            operation.equals("set", true)
    }
}
