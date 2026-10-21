package com.gitee.planners.module.compat.zaphkiel

import com.gitee.planners.api.item.ItemProvider
import com.gitee.planners.api.item.ItemProviders
import ink.ptms.zaphkiel.Zaphkiel
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

    /** 注册到 Planners 物品库注册表。 */
    fun register() {
        ItemProviders.register(this)
    }
}
