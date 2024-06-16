package com.gitee.planners.api

import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.*
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.api.profile.ProfileOperatorImpl
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.module.magic.MagicPoint.magicPoint
import com.gitee.planners.module.magic.MagicPoint.magicPointInUpperLimit
import com.gitee.planners.util.builtin.MutableRegistryInMap
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.onlinePlayers
import java.util.UUID

object ProfileAPI : MutableRegistryInMap<UUID, PlayerProfile>() {

    val OPERATOR = ProfileOperatorImpl()

    val Player.plannersLoaded: Boolean
        get() = containsKey(uniqueId)

    val Player.plannersProfile: PlayerProfile
        get() = getOrNull(this.uniqueId) ?: error("Player $name unloaded.")

    fun addMagicPoint(player: Player, amount: Int) {
        val event = PlayerMagicPointEvent.Increase(player.plannersProfile, amount)
        if (event.call()) {
            player.plannersProfile.magicPoint += amount
        }
    }

    fun takeMagicPoint(player: Player, amount: Int) {
        val event = PlayerMagicPointEvent.Decrease(player.plannersProfile, amount)
        if (event.call()) {
            player.plannersProfile.magicPoint -= amount
        }
    }

    fun setMagicPoint(player: Player, to: Int) {
        val event = PlayerMagicPointEvent.Set(player.plannersProfile, to)
        if (event.call()) {
            player.plannersProfile.magicPoint = to
        }
    }

    fun resetMagicPoint(player: Player) {
        setMagicPoint(player, player.plannersProfile.magicPointInUpperLimit)
    }

    fun addLevel(player: Player, amount: Int) {
        addLevel(player.plannersProfile, amount)
    }

    fun setLevel(player: Player, to: Int) {
        setLevel(player.plannersProfile, to)
    }

    fun addExperience(player: Player, amount: Int) {
        addExperience(player.plannersProfile, amount)
    }

    fun setExperience(player: Player, value: Int) {
        setExperience(player.plannersProfile, value)
    }

    fun takeExperience(player: Player, amount: Int) {
        takeExperience(player.plannersProfile, amount)
    }

    fun addLevel(profile: PlayerProfile, amount: Int) {
        val event = PlayerLevelChangeEvent(profile, profile.level, profile.level + amount)
        if (event.call()) {
            profile.setLevel(profile.level + amount)
        }
    }

    fun setLevel(profile: PlayerProfile, to: Int) {
        val event = PlayerLevelChangeEvent(profile, profile.level, to)
        if (event.call()) {
            profile.setLevel(to)
        }
    }

    fun addExperience(profile: PlayerProfile, amount: Int) {
        val event = PlayerExperienceEvent.Increment(profile, amount)
        if (event.call()) {
            profile.addExperience(amount).thenAccept {
                PlayerExperienceEvent.Updated(profile).call()
            }
        }
    }

    fun setExperience(profile: PlayerProfile, value: Int) {
        val event = PlayerExperienceEvent.Set(profile, value)
        if (event.call()) {
            profile.setExperience(value)
            PlayerExperienceEvent.Updated(profile).call()
        }
    }

    fun takeExperience(profile: PlayerProfile, amount: Int) {
        val event = PlayerExperienceEvent.Decrement(profile, amount)
        if (event.call()) {
            profile.takeExperience(amount).thenAccept {
                PlayerExperienceEvent.Updated(profile).call()
            }
        }
    }

    fun setSkillLevel(profile: PlayerProfile, skill: PlayerSkill, to: Int) {
        if (PlayerSkillEvent.LevelChange(profile, skill, skill.level, to).call()) {
            skill.level = to
        }
    }

    fun setSkillBinding(profile: PlayerProfile, skill: PlayerSkill, binding: KeyBinding?) {
        PlayerSkillEvent.BindingChange(profile, skill, binding).call()
        // 如果 binding 为 null 代表解绑
        if (binding == null) {
            skill.binding = null
        }
        // 解绑相同的快捷键的技能
        else {
            val registriedSkill = profile.getRegisteredSkillOrNull(binding)
            if (registriedSkill != null) {
                setSkillBinding(profile, registriedSkill, null)
            }
            skill.binding = binding
        }
    }

    /**
     * 加载
     */
    @SubscribeEvent
    private fun handleProfileLinked(e: PlayerJoinEvent) {
        submitAsync(delay = 5) {
            if (e.player.isOnline) {
                val profile = Database.INSTANCE.getPlayerProfile(e.player)
//                 更新到玩家默认技能实例
                profile.executeUpdatedDefaultSkill().thenAccept {
                    this@ProfileAPI[e.player.uniqueId] = profile
                    PlayerProfileLoadedEvent(profile).call()
                }
            }
        }
    }

    /**
     * 同步更新配置节能
     */
    @SubscribeEvent
    fun e(e: PluginReloadEvents.Post) {
        submitAsync {
            this@ProfileAPI.values().forEach { it.executeUpdatedDefaultSkill() }
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
