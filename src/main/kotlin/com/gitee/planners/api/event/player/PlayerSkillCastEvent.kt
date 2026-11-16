package com.gitee.planners.api.event.player

import com.gitee.planners.api.directing.DirectingFailureReason
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
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

    /**
     * 释放前条件校验失败事件（资源、冷却等）。
     *
     * 默认的聊天提示仍由 Planners 发出；监听方只负责额外反馈（按键槽位、音效等）。
     *
     * @param player 玩家
     * @param skill 技能
     * @param failure 失败的校验与上下文
     */
    class PreConditionFailed(
        val player: Player,
        val skill: PlayerSkill,
        val failure: CastPreConditionResult.Failure,
    ) : BukkitProxyEvent()

    /**
     * 指向选择失败事件（缺少运行期会话、落点非法、超时）。
     *
     * 玩家主动取消、被新会话替换或离线不派发该事件。
     *
     * @param player 玩家
     * @param skill 技能
     * @param sourceKey 本次按键标识
     * @param reason 失败原因
     */
    class DirectingFailed(
        val player: Player,
        val skill: PlayerSkill,
        val sourceKey: String,
        val reason: DirectingFailureReason,
    ) : BukkitProxyEvent()

    companion object {

        /**
         * 事件派发入口；单元测试可替换为记录器，避免依赖 Bukkit 事件总线。
         */
        internal var emitter: (BukkitProxyEvent) -> Unit = { event -> event.call() }

        /**
         * 派发一个事件。
         *
         * @param event 待派发事件
         */
        internal fun emit(event: BukkitProxyEvent) {
            emitter(event)
        }
    }
}
