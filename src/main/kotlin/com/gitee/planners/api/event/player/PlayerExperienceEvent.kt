package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerProfile
import taboolib.platform.type.BukkitProxyEvent

abstract class PlayerExperienceEvent(val profile: PlayerProfile) : BukkitProxyEvent() {

    val player = profile.onlinePlayer

    class Increment(profile: PlayerProfile, var amount: Int) : PlayerExperienceEvent(profile)

    class Decrement(profile: PlayerProfile, var amount: Int) : PlayerExperienceEvent(profile)

    class Set(profile: PlayerProfile, var value: Int) : PlayerExperienceEvent(profile)

    class Updated(profile: PlayerProfile) : PlayerExperienceEvent(profile)

}
