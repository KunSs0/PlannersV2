package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerSkill
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
     * 玩家技能输入检查事件（Pre 之前）
     *
     * 可用于拦截技能输入并接管后续流程（如播放招式动画后继续释放）。
     * 取消此事件将阻止技能释放。
     *
     * @param player 玩家
     * @param skill 技能
     */
    class Check(val player: Player, val skill: PlayerSkill) : BukkitProxyEvent()

    /**
     * 玩家技能释放前事件
     *
     * @param player 玩家
     * @param skill 技能
     */
    class Pre(val player: Player, val skill: PlayerSkill) : BukkitProxyEvent()

    /**
     * 玩家技能释放后事件
     *
     * @param player 玩家
     * @param skill 技能
     */
    class Post(val player: Player, val skill: PlayerSkill) : BukkitProxyEvent()

}
