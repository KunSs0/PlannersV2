package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

/**
 * 职业等级已提交的通知，不参与修改前的取消或目标等级调整。
 *
 * @property template 已完成等级修改的档案。
 * @property from 修改前等级。
 * @property to 写入并经过等级边界约束后的实际等级。
 */
class PlayerLevelChangedEvent(val template: PlayerTemplate, val from: Int, val to: Int) : BukkitProxyEvent() {

    val player = template.onlinePlayer

    override val allowCancelled: Boolean
        get() = false
}
