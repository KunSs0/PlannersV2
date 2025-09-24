package com.gitee.planners.module.compat.dragoncore

import com.gitee.planners.api.event.ProxyClientKeyEvents
import com.gitee.planners.util.checkPlugin
import eos.moe.dragoncore.api.event.KeyPressEvent
import eos.moe.dragoncore.api.event.KeyReleaseEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerBukkitListener

object DragonCoreKeyHandler {

    @Awake(LifeCycle.ENABLE)
    fun init() {
        if (checkPlugin("DragonCore")) {
            info("Loaded DragonCore key event support.")
            registerBukkitListener(KeyPressEvent::class.java) {
                val player = it.player
                val key = it.key
                ProxyClientKeyEvents.Down(player, key).call()
            }

            registerBukkitListener(KeyReleaseEvent::class.java) {
                val player = it.player
                val key = it.key
                ProxyClientKeyEvents.Up(player, key).call()
            }
        }
    }


}