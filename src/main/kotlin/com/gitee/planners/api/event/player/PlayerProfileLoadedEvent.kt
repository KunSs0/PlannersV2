package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerProfile
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class PlayerProfileLoadedEvent(val profile: PlayerProfile) : BukkitProxyEvent() {

    val player: Player = profile.onlinePlayer

    override val allowCancelled: Boolean
        get() = false

}
