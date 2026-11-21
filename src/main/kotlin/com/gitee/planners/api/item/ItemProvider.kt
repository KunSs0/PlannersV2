package com.gitee.planners.api.item

import com.gitee.planners.core.skill.condition.CastCostReservation
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 物品库适配器，把 `provider:item-id` 形式的物品引用映射到具体物品。
 *
 * 适配器由对应物品库插件在启用期注册，学习条件只按 provider 段查表，不感知具体物品库实现；
 * 数量统计、扣除与显示名是同一份体系能力，按需覆盖，默认视为不支持（由调用方在加载期报错）。
 */
interface ItemProvider {

    /** 物品库 ID，对应物品引用中 `provider:` 段。 */
    val providerId: String

    /**
     * 判断物品堆是否为该物品库中的指定物品。
     *
     * @param itemStack 待检查的物品。
     * @param itemId 物品库内部 ID。
     * @return 匹配时返回 true。
     */
    fun matches(itemStack: ItemStack, itemId: String): Boolean

    /**
     * 该物品库是否支持按 ID 统计与扣除。
     *
     * @param itemId 物品库内部 ID。
     * @return 支持时返回 true。
     */
    fun supportsConsume(itemId: String): Boolean {
        return false
    }

    /**
     * 统计玩家持有的物品数量。
     *
     * @param player 玩家。
     * @param itemId 物品库内部 ID。
     * @return 持有数量。
     */
    fun count(player: Player, itemId: String): Int {
        throw UnsupportedOperationException("物品库 '$providerId' 不支持数量统计")
    }

    /**
     * 准备一次扣除，不修改玩家数据。
     *
     * @param player 玩家。
     * @param itemId 物品库内部 ID。
     * @param amount 扣除数量，调用方已保证大于 0。
     * @return 支持 commit/rollback 的预留句柄。
     */
    fun prepareConsume(player: Player, itemId: String, amount: Int): CastCostReservation {
        throw UnsupportedOperationException("物品库 '$providerId' 不支持扣除")
    }

    /**
     * 物品显示名，用于失败提示。
     *
     * @param itemId 物品库内部 ID。
     * @return 显示名；无显示名时返回 null，由调用方回退成 `provider:itemId`。
     */
    fun displayName(itemId: String): String? {
        return null
    }
}
