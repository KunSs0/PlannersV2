package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

/**
 * 主线程职业路由、有效技能和属性提交清空后的事实通知，不可取消。
 *
 * 数据库删除仍异步执行，监听器不能将此事件视为持久化成功通知。
 *
 * @property template 已提交清空的当前玩家档案。
 * @property previousBindingId 被清空的旧路由记录 ID。
 * @property previousRouterId 被清空的职业线配置 ID。
 */
class PlayerRouteClearedEvent(
    val template: PlayerTemplate,
    val previousBindingId: Long,
    val previousRouterId: String
) : BukkitProxyEvent() {

    /**
     * 接收本次清空完成通知的玩家。
     */
    val player = template.onlinePlayer

    /**
     * 清空已提交，监听器不能取消该事实。
     */
    override val allowCancelled: Boolean
        get() {
            return false
        }
}
