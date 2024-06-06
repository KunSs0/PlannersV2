package com.gitee.planners.core.skill.cooler

import com.gitee.planners.Planners
import com.gitee.planners.api.job.Skill
import org.bukkit.entity.Player
import taboolib.common.util.unsafeLazy

interface Cooler {

    companion object {

        /**
         * 只在启动后第一次初始化,如果想重新初始化,必须要重启
         */
        val INSTANCE by unsafeLazy {
            when (Planners.config.getString("settings.cooler.memory", "memory")!!.lowercase()) {
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
    fun set(player: Player, skill: Skill, durationTick: Int)

    /**
     * 获取玩家技能冷却到期时间 单位tick
     * 如果技能没有冷却则返回 -1
     * @param player 玩家
     * @param skill 技能
     * @return 冷却剩余时间
     */
    fun get(player: Player, skill: Skill): Long

}
