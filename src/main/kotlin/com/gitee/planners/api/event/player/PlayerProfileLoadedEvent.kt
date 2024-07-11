package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class PlayerProfileLoadedEvent(val template: PlayerTemplate) : BukkitProxyEvent() {

    val player: Player = template.onlinePlayer

    override val allowCancelled: Boolean
        get() = false

}
