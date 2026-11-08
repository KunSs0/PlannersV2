package com.gitee.planners.core.skill.condition

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.context.SkillExecutionContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * `item_holy_emblem`：圣徽仪式的施法条件与扣费。
 *
 * 施法前要求背包里有足够数量的圣徽，提交阶段按快照扣除；
 * 只统计主背包与快捷栏，不看装备与副手。
 *
 * 可配置参数（`props`）：
 * - `item`：Zaphkiel 物品 ID，默认 `holy_emblem`；
 * - `amount`：需要数量，默认 1。
 */
class HolyEmblemCastCondition : SkillCastCondition {

    override fun verify(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        props: Map<String, Any>,
    ): String? {
        require(Bukkit.isPrimaryThread()) { "圣徽条件必须在主线程执行" }
        val item = itemId(props)
        val required = amount(props)
        val owned = ZaphkielItemCost.count(player, item)
        if (owned < required) {
            return "需要" + required + "枚圣徽"
        }
        return null
    }

    override fun prepareConsume(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        props: Map<String, Any>,
    ): CastCostReservation? {
        require(Bukkit.isPrimaryThread()) { "圣徽扣费必须在主线程执行" }
        return ZaphkielItemCost.prepare(player, itemId(props), amount(props))
    }

    /**
     * 读取物品 ID。
     *
     * @param props 条件参数。
     * @return 物品 ID。
     */
    private fun itemId(props: Map<String, Any>): String {
        val value = props["item"]
        if (value == null) {
            return DEFAULT_ITEM
        }
        val text = value.toString().trim()
        if (text.isEmpty()) {
            return DEFAULT_ITEM
        }
        return text
    }

    /**
     * 读取需要数量。
     *
     * @param props 条件参数。
     * @return 数量。
     * @throws IllegalArgumentException 数量不是正整数时抛出。
     */
    private fun amount(props: Map<String, Any>): Int {
        val value = props["amount"]
        if (value == null) {
            return DEFAULT_AMOUNT
        }
        val number = when (value) {
            is Number -> value.toInt()
            else -> value.toString().trim().toIntOrNull()
        }
        require(number != null && number > 0) { "圣徽数量必须为正整数，实际 $value" }
        return number
    }

    companion object {

        /** 条件注册 ID，与技能配置里的 `item_holy_emblem` 对应。 */
        const val ID = "item_holy_emblem"

        /** 默认物品 ID。 */
        private const val DEFAULT_ITEM = "holy_emblem"

        /** 默认数量。 */
        private const val DEFAULT_AMOUNT = 1
    }
}
