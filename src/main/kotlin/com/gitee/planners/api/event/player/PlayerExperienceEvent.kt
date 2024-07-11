package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

abstract class PlayerExperienceEvent(val template: PlayerTemplate) : BukkitProxyEvent() {

    val player = template.onlinePlayer

    class Increment(template: PlayerTemplate, var amount: Int) : PlayerExperienceEvent(template)

    class Decrement(template: PlayerTemplate, var amount: Int) : PlayerExperienceEvent(template)

    class Set(template: PlayerTemplate, var value: Int) : PlayerExperienceEvent(template)

    class Updated(template: PlayerTemplate) : PlayerExperienceEvent(template)

}
