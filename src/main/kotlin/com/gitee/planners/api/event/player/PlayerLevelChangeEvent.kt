package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

class PlayerLevelChangeEvent(val template: PlayerTemplate, val form: Int, var to: Int) : BukkitProxyEvent() {

    val player = template.onlinePlayer

}
