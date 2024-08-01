package com.gitee.planners.api.event.player

import com.gitee.planners.api.job.Skill
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class PlayerSkillCooldownEvent {

    /**
     * 玩家技能冷却被设置事件
     * @param player 玩家
     * @param skill 技能
     * @param ticks 冷却时间
     */
    class Set(val player: Player, val skill: Skill, var ticks: Int) : BukkitProxyEvent()

}
