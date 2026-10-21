package com.gitee.planners.api.item

import org.bukkit.inventory.ItemStack

/**
 * 物品库适配器，把 `provider:item-id` 形式的物品引用映射到具体物品。
 *
 * 适配器由对应物品库插件在启用期注册，学习条件只按 provider 段查表，不感知具体物品库实现。
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
}
