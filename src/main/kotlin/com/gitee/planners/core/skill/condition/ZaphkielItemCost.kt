package com.gitee.planners.core.skill.condition

import ink.ptms.zaphkiel.Zaphkiel
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Array

/**
 * Zaphkiel 真实物品的跨堆栈扣费句柄。
 *
 * 只在主线程工作：准备阶段只记录快照、不改背包；提交时先校验背包没有被外部改动，
 * 再逐槽写入；回滚只恢复本句柄改动过的槽位，支持重复提交与中途失败。
 *
 * @property player 扣费玩家。
 * @property slots 涉及的槽位快照。
 */
class ZaphkielItemCost(
    private val player: Player,
    private val slots: List<SlotSnapshot>,
) : CastCostReservation {

    /** 是否已经写入过背包。 */
    private var attempted = false

    /** 是否已经结束，结束后不再改动背包。 */
    private var closed = false

    override fun commit() {
        require(Bukkit.isPrimaryThread()) { "物品消费必须在主线程执行" }
        require(!closed) { "物品消费句柄已结束" }
        val inventory = player.inventory
        // 先统一校验，再统一写入；准备后背包发生变化时拒绝覆盖。
        for (entry in slots) {
            require(inventory.getItem(entry.slot) == entry.before) { "消费物品已变化" }
        }
        attempted = true
        for (entry in slots) {
            inventory.setItem(entry.slot, entry.after)
        }
    }

    override fun rollback() {
        require(Bukkit.isPrimaryThread()) { "物品回滚必须在主线程执行" }
        if (closed) {
            return
        }
        if (attempted) {
            val inventory = player.inventory
            for (entry in slots) {
                val current = inventory.getItem(entry.slot)
                require(current == entry.after || current == entry.before) { "消费后背包发生变化，拒绝覆盖" }
            }
            for (entry in slots) {
                if (inventory.getItem(entry.slot) == entry.after) {
                    inventory.setItem(entry.slot, entry.before)
                }
            }
        }
        closed = true
    }

    /**
     * 单个槽位的消费前后快照。
     *
     * @property slot 槽位下标。
     * @property before 准备时的物品。
     * @property after 提交后的物品；为 null 表示该槽清空。
     */
    class SlotSnapshot(val slot: Int, val before: ItemStack, val after: ItemStack?)

    companion object {

        /**
         * 统计主背包与快捷栏里指定 Zaphkiel 物品的数量。
         *
         * 只统计存储槽位，不误扣装备、副手或鼠标物品。
         *
         * @param player 玩家。
         * @param itemId Zaphkiel 物品 ID。
         * @return 数量。
         */
        fun count(player: Player, itemId: String): Int {
            require(Bukkit.isPrimaryThread()) { "读取物品必须在主线程执行" }
            var total = 0
            val contents = player.inventory.storageContents
            var index = 0
            while (index < Array.getLength(contents)) {
                val stack = contents[index]
                index += 1
                if (stack == null) {
                    continue
                }
                if (!matches(stack, itemId)) {
                    continue
                }
                total += stack.amount
            }
            return total
        }

        /**
         * 准备跨堆栈扣除快照，不修改背包。
         *
         * @param player 玩家。
         * @param itemId Zaphkiel 物品 ID。
         * @param amount 需要扣除的数量。
         * @return 扣费句柄。
         * @throws IllegalArgumentException 数量不足时抛出。
         */
        fun prepare(player: Player, itemId: String, amount: Int): ZaphkielItemCost {
            require(Bukkit.isPrimaryThread()) { "准备物品消费必须在主线程执行" }
            require(amount > 0) { "物品数量必须为正" }
            require(itemId.isNotEmpty()) { "物品ID不能为空" }
            val slots = ArrayList<SlotSnapshot>()
            val inventory = player.inventory
            var remaining = amount
            val contents = inventory.storageContents
            var slot = 0
            while (slot < Array.getLength(contents)) {
                val stack = contents[slot]
                if (stack != null && matches(stack, itemId)) {
                    val taken = minOf(remaining, stack.amount)
                    val before = stack.clone()
                    var after: ItemStack? = null
                    if (stack.amount > taken) {
                        val partial = stack.clone()
                        partial.amount = stack.amount - taken
                        after = partial
                    }
                    slots.add(SlotSnapshot(slot, before, after))
                    remaining -= taken
                    if (remaining == 0) {
                        break
                    }
                }
                slot += 1
            }
            require(remaining == 0) { "缺少物品: $itemId" }
            return ZaphkielItemCost(player, slots)
        }

        /**
         * 按 Zaphkiel 身份匹配物品，不通过材质或显示名识别。
         *
         * @param stack 物品堆。
         * @param itemId Zaphkiel 物品 ID。
         * @return 匹配时返回 true。
         */
        private fun matches(stack: ItemStack, itemId: String): Boolean {
            if (stack.amount <= 0) {
                return false
            }
            return Zaphkiel.api().getItemHandler().getItemId(stack) == itemId
        }
    }
}
