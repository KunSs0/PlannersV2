package com.gitee.planners.api

import com.gitee.planners.api.common.registry.AbstractRegistry
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.profile.ProfileOperatorImpl
import com.gitee.planners.api.profile.ProfileOperator
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerProfile
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.onlinePlayers
import java.util.UUID

object ProfileAPI : AbstractRegistry<UUID, PlayerProfile>() {

    val OPERATOR = ProfileOperatorImpl()

    val Player.plannersLoaded: Boolean
        get() = containsKey(uniqueId)

    val Player.plannersProfile: PlayerProfile
        get() = getOrNull(this.uniqueId) ?: error("Player $name unloaded.")

    /**
     * 加载
     */
    @SubscribeEvent
    private fun handleProfileLinked(e: PlayerJoinEvent) {
        submitAsync(delay = 5) {
            if (e.player.isOnline) {
                val profile = Database.INSTANCE.getPlayerProfile(e.player)
                this@ProfileAPI[e.player.uniqueId] = profile
                PlayerProfileLoadedEvent(profile).call()
            }
        }
    }

    /**
     * 保存 metadata 资源
     */
    @Schedule(async = true, period = 20)
    private fun handleMetadataUpdater() {
        onlinePlayers.forEach { player ->
            val profile = this.getOrNull(player.uniqueId) ?: return@forEach
            profile.release().forEach { (key, data) ->
                Database.INSTANCE.updateMetadata(profile, key, data)
            }
        }
    }


}
