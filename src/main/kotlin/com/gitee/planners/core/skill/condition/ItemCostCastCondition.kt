package com.gitee.planners.core.skill.condition

import com.gitee.planners.api.item.ItemProviders
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.gitee.planners.module.action.ActionScopeFactory
import com.gitee.planners.module.action.query.QueryRef
import org.bukkit.entity.Player
import taboolib.platform.util.asLangText

/**
 * 通用物品成本条件 `item_cost`。
 *
 * 配置只描述"扣哪个体系的哪个物品、扣多少"：
 *
 * ```yaml
 * item_cost:
 *   props:
 *     item: zap:holy_emblem
 *     amount: "<skill.level>"
 * ```
 *
 * `item` 必填且必须是 `provider:itemId`；`amount` 必填，可以是字面量、路径或表达式，
 * 求值后必须是正整数。物品体系由 [ItemProvider] 注册表提供，条件本身不认识任何具体插件。
 */
class ItemCostCastCondition : SkillCastCondition {

    override fun validate(props: Map<String, Any>, path: String) {
        val reference = ItemReference.parse(props[PROP_ITEM], "$path.props.$PROP_ITEM")
        val provider = ItemProviders.getOrNull(reference.provider)
        require(provider != null) {
            "$path.props.$PROP_ITEM 的物品提供者 '${reference.provider}' 未注册，可用：${ItemProviders.ids()}"
        }
        require(provider.supportsConsume(reference.itemId)) {
            "$path.props.$PROP_ITEM 的物品 '${reference.itemId}' 在提供者 '${reference.provider}' 中不存在或不支持数量扣除"
        }
        val amount = props[PROP_AMOUNT]
        require(amount != null) { "$path.props.$PROP_AMOUNT 不能为空" }
        val literal = parseLiteralAmount(amount)
        if (literal != null) {
            require(literal > 0) { "$path.props.$PROP_AMOUNT 必须是正整数，实际：$literal" }
        }
    }

    override fun verify(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        props: Map<String, Any>
    ): String? {
        val reference = ItemReference.parse(props[PROP_ITEM], "item_cost.item")
        val provider = ItemProviders.getOrNull(reference.provider)
            ?: return "物品提供者不可用：${reference.provider}"
        val amount = resolveAmount(skill, execution, props)
        val owned = provider.count(player, reference.itemId)
        if (owned >= amount) {
            return null
        }
        val label = props[PROP_DISPLAY_NAME]?.toString() ?: provider.displayName(reference.itemId) ?: reference.raw
        return player.asLangText("cast-condition-item-cost", label, amount)
    }

    override fun prepareConsume(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        props: Map<String, Any>
    ): CastCostReservation {
        val reference = ItemReference.parse(props[PROP_ITEM], "item_cost.item")
        val provider = ItemProviders.getOrNull(reference.provider)
            ?: throw IllegalStateException("物品提供者不可用：${reference.provider}")
        val amount = resolveAmount(skill, execution, props)
        return provider.prepareConsume(player, reference.itemId, amount)
    }

    /**
     * 求值本次需要扣除的数量。
     *
     * 同一次释放只求值一次并缓存进执行上下文，保证校验与提交读到同一个数量。
     *
     * @param skill 技能。
     * @param execution 执行上下文。
     * @param props 条件参数。
     * @return 正整数数量。
     */
    private fun resolveAmount(
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        props: Map<String, Any>
    ): Int {
        val key = CACHE_KEY + skill.id
        val cached = execution.getVariable(key)
        if (cached is Int) {
            return cached
        }
        val amount = props[PROP_AMOUNT]
        val literal = parseLiteralAmount(amount)
        val resolved: Int
        if (literal != null) {
            resolved = literal
        } else {
            val text = amount?.toString() ?: throw IllegalArgumentException("item_cost.amount 不能为空")
            val scope = ActionScopeFactory.forExecution(execution, LinkedHashMap(), emptyMap())
            val value = QueryRef.parse(text, "item_cost.amount").resolveDouble(scope)
            require(value > 0 && value % 1.0 == 0.0) {
                "item_cost.amount 必须是正整数，实际：$value"
            }
            resolved = value.toInt()
        }
        execution.setVariable(key, resolved)
        return resolved
    }

    /**
     * 解析字面量数量。
     *
     * @param value 配置值。
     * @return 整数字面量；需要表达式求值时返回 null。
     */
    private fun parseLiteralAmount(value: Any?): Int? {
        if (value is Number) {
            val number = value.toDouble()
            if (number % 1.0 != 0.0) {
                throw IllegalArgumentException("item_cost.amount 必须是正整数，实际：$value")
            }
            return number.toInt()
        }
        val text = value?.toString()?.trim() ?: return null
        if (text.isEmpty()) {
            throw IllegalArgumentException("item_cost.amount 不能为空")
        }
        return text.toIntOrNull()
    }

    private companion object {

        /** 参数名：物品引用。 */
        const val PROP_ITEM = "item"

        /** 参数名：数量。 */
        const val PROP_AMOUNT = "amount"

        /** 参数名：显示名覆盖；为空时读物品库显示名。 */
        const val PROP_DISPLAY_NAME = "display.name"

        /** 数量缓存键前缀。 */
        const val CACHE_KEY = "__item_cost_amount_"
    }
}
