package com.gitee.planners.api.event.script

import com.gitee.planners.api.job.target.ProxyTarget
import taboolib.platform.type.BukkitProxyEvent

class ScriptCustomTriggerEvent(val sender: ProxyTarget<*>, val name: String) : BukkitProxyEvent() {
}
