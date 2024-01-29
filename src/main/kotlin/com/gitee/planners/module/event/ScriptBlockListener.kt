package com.gitee.planners.module.event

import com.gitee.planners.api.common.script.ComplexCompiledScript
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.library.kether.Quest

class ScriptBlockListener(
    val id: String,
    val compiled: ComplexCompiledScript,
    val block: Quest.Block,
    val event: String,
    val priority: EventPriority = EventPriority.NORMAL,
    val ignoreCancelled: Boolean = true,
    val async: Boolean = true
) {


    lateinit var mapping : ProxyListener

    override fun toString(): String {
        return "ScriptBlockListener(id='$id', compiled=$compiled, block=$block, event='$event', priority=$priority, ignoreCancelled=$ignoreCancelled)"
    }


}
