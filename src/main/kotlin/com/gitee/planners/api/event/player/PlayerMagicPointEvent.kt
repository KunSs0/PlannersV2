package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

abstract class PlayerMagicPointEvent(val template: PlayerTemplate) : BukkitProxyEvent() {

    val player = template.onlinePlayer

    class Increase(template: PlayerTemplate, var amount: Int) : PlayerMagicPointEvent(template)

    class Decrease(template: PlayerTemplate, var amount: Int) : PlayerMagicPointEvent(template)

    class Set(template: PlayerTemplate, var value: Int) : PlayerMagicPointEvent(template)

}
