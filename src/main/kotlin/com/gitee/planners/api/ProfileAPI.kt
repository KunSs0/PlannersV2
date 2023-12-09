package com.gitee.planners.api

import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.profile.ProfileOperatorImpl
import com.gitee.planners.api.profile.ProfileOperator
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerProfile
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import java.util.UUID

object ProfileAPI {

    private val profiles = mutableMapOf<UUID, PlayerProfile>()

    val Player.plannersLoaded : Boolean
        get() = profiles.containsKey(uniqueId)

    val Player.plannersProfile: PlayerProfile
        get() = getProfile(this)

    fun getProfile(player: Player): PlayerProfile {
        return this.profiles[player.uniqueId] ?: error("Player ${player.name} unloaded.")
    }

    @SubscribeEvent
    private fun e(e: PlayerJoinEvent) {
        submitAsync(delay = 5) {
            if (e.player.isOnline) {
                this@ProfileAPI.profiles[e.player.uniqueId] = Database.INSTANCE.getPlayerProfile(e.player).also {
                    PlayerProfileLoadedEvent(it).call()
                }
            }
        }
    }

    fun modified(player: Player, async: Boolean = true, block: ProfileOperator.() -> Unit) {
        val profile = profiles[player.uniqueId] ?: error("Player ${player.name} unloaded.")
        if (async) {
            submitAsync { block(ProfileOperatorImpl(profile)) }
        } else {
            block(ProfileOperatorImpl(profile))
        }
    }

}
