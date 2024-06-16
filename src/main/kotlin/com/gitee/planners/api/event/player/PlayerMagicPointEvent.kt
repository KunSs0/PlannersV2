package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerProfile
import taboolib.platform.type.BukkitProxyEvent

abstract class PlayerMagicPointEvent(val profile: PlayerProfile) : BukkitProxyEvent() {

    val player = profile.onlinePlayer

    class Increase(profile: PlayerProfile, var amount: Int) : PlayerMagicPointEvent(profile)

    class Decrease(profile: PlayerProfile, var amount: Int) : PlayerMagicPointEvent(profile)

    class Set(profile: PlayerProfile, var value: Int) : PlayerMagicPointEvent(profile)

}
