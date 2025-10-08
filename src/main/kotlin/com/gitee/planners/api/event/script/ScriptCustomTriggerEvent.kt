package com.gitee.planners.api.event.script

import com.gitee.planners.api.job.target.Target
import taboolib.platform.type.BukkitProxyEvent

class ScriptCustomTriggerEvent(val sender: Target<*>, val name: String) : BukkitProxyEvent() {
}