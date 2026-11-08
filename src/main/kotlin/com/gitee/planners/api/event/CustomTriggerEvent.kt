package com.gitee.planners.api.event

import com.gitee.planners.api.job.target.ProxyTarget
import taboolib.platform.type.BukkitProxyEvent

/**
 * 自定义触发事件。
 *
 * 由管理员命令或 MythicMobs mechanic 广播，供扩展监听；
 * 它不会自动调用状态的生命周期流程。
 *
 * @property sender 触发该事件的实体。
 * @property name 触发名。
 */
class CustomTriggerEvent(val sender: ProxyTarget<*>, val name: String) : BukkitProxyEvent() {
}