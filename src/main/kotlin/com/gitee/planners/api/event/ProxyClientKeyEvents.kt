package com.gitee.planners.api.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * ProxyClientKeyEvents
 *
 *
 */
class ProxyClientKeyEvents {

    class Up(val sender: Player,val key: String) : BukkitProxyEvent()

    class Down(val sender: Player,val key: String) : BukkitProxyEvent()

}