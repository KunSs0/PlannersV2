package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class PlayerLevelChangeEvent(val profile: PlayerProfile, val form: Int, var to: Int) : BukkitProxyEvent() {

    val player = profile.onlinePlayer

}
