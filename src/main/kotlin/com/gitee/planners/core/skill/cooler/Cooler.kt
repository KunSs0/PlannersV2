package com.gitee.planners.core.skill.cooler

import com.gitee.planners.Planners
import com.gitee.planners.api.common.Unique
import org.bukkit.entity.Player
import taboolib.common.util.unsafeLazy

interface Cooler {

    companion object {

        /**
         * 冷却存储实现。
         *
         * 由 `settings.cooler.use` 选择 `memory`（重启清空）或 `persistence`（写入数据库）；
         * 只在启动后第一次初始化，修改配置需要重启。
         */
        val INSTANCE by unsafeLazy {
            when (Planners.config.getString("settings.cooler.use", "memory")!!.lowercase()) {
                "memory" -> MemoryCooler()
                "persistence" -> PersistenceCooler()
                else -> error("Unknown cooler type.")
            }
        }

    }

    /**
     * 设置玩家技能冷却 单位tick
     * @param player 玩家
     * @param skill 技能
     * @param durationTick 冷却时间
     */
    fun set(player: Player, skill: Unique, durationTick: Int)

    /**
     * 获取玩家技能冷却到期时间 单位tick
     * 如果技能没有冷却则返回 -1
     * @param player 玩家
     * @param skill 技能
     * @return 冷却剩余时间
     */
    fun get(player: Player, skill: Unique): Long

}
