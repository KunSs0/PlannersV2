package com.gitee.planners.api.event.player

import com.gitee.planners.api.job.Skill
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * @Author KunSs
 * @Date 2024/9/17 19:15
 * @version 1.0
 *
 */
class PlayerSkillCastEvent {

    /**
     * 玩家技能释放前事件
     *
     * @param player 玩家
     * @param skill 技能
     */
    class Pre(val player: Player, val skill: Skill) : BukkitProxyEvent()

    /**
     * 玩家技能释放后事件
     *
     * @param player 玩家
     * @param skill 技能
     */
    class Post(val player: Player, val skill: Skill) : BukkitProxyEvent()

}
