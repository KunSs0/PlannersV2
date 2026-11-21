package com.gitee.planners.module.compat.zaphkiel

import com.gitee.planners.api.item.ItemProvider
import com.gitee.planners.api.item.ItemProviders
import com.gitee.planners.core.skill.condition.CastCostReservation
import ink.ptms.zaphkiel.Zaphkiel
import ink.ptms.zaphkiel.api.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Zaphkiel 物品库适配器，配置中通过 `zap:<物品 ID>` 引用 Zaphkiel 物品。
 *
 * 仅在 Zaphkiel 启用后注册；未注册时对应物品条件始终不满足，不影响插件启用。
 */
object ZaphkielItemProvider : ItemProvider {

    override val providerId: String = "zap"

    override fun matches(itemStack: ItemStack, itemId: String): Boolean {
        return Zaphkiel.api().getItemHandler().getItemId(itemStack) == itemId
    }

    override fun supportsConsume(itemId: String): Boolean {
        return item(itemId) != null
    }

    override fun count(player: Player, itemId: String): Int {
        return ZaphkielItemReservation.count(player, itemId)
    }

    override fun prepareConsume(player: Player, itemId: String, amount: Int): CastCostReservation {
        return ZaphkielItemReservation.prepare(player, itemId, amount)
    }

    override fun displayName(itemId: String): String? {
        val item = item(itemId) ?: return null
        val names = item.name
        val default = names["default"]
        if (default != null && default.isNotBlank()) {
            return default
        }
        for (value in names.values) {
            if (value.isNotBlank()) {
                return value
            }
        }
        return null
    }

    /** 注册到 Planners 物品库注册表。 */
    fun register() {
        ItemProviders.register(this)
    }

    /**
     * 按 ID 读取 Zaphkiel 物品定义。
     *
     * @param itemId 物品 ID。
     * @return 物品定义；不存在时返回 null。
     */
    private fun item(itemId: String): Item? {
        return Zaphkiel.api().getItemManager().getItem(itemId)
    }
}
