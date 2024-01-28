package com.gitee.planners.api

import com.gitee.planners.api.common.registry.AbstractRegistry
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.api.profile.ProfileOperatorImpl
import com.gitee.planners.api.profile.ProfileOperator
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.database.DatabaseSQL
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.onlinePlayers
import java.util.UUID
import java.util.concurrent.CompletableFuture

object ProfileAPI : AbstractRegistry<UUID, PlayerProfile>() {

    val OPERATOR = ProfileOperatorImpl()

    val Player.plannersLoaded: Boolean
        get() = containsKey(uniqueId)

    val Player.plannersProfile: PlayerProfile
        get() = getOrNull(this.uniqueId) ?: error("Player $name unloaded.")

    /**
     * 请求修改技能等级 会经过事件
     */
    fun requestModifiedSkillLevel(profile: PlayerProfile, skill: PlayerSkill, to: Int) {
        if (PlayerSkillEvent.LevelChange(profile, skill, skill.level, to).call()) {
            skill.level = to
        }
    }

    fun requestModifiedSkillBinding(profile: PlayerProfile, skill: PlayerSkill, binding: KeyBinding?) {
        PlayerSkillEvent.BindingChange(profile, skill, binding).call()
        // 如果 binding 为 null 代表解绑
        if (binding == null) {
            skill.binding = null
        }
        // 解绑相同的快捷键的技能
        else {
            val registriedSkill = profile.getRegistriedSkillOrNull(binding)
            if (registriedSkill != null) {
                requestModifiedSkillBinding(profile, registriedSkill, null)
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
                // 更新到玩家默认技能实例
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
            this@ProfileAPI.getValues().forEach { it.executeUpdatedDefaultSkill() }
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
