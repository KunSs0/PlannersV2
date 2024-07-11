package com.gitee.planners.api

import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.*
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.api.template.ProfileOperatorImpl
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerTemplate
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

object PlayerTemplateAPI : MutableRegistryInMap<UUID, PlayerTemplate>() {

    val OPERATOR = ProfileOperatorImpl()

    val Player.plannersLoaded: Boolean
        get() = containsKey(uniqueId)

    val Player.plannersTemplate: PlayerTemplate
        get() = getOrNull(this.uniqueId) ?: error("Player $name unloaded.")

    fun addMagicPoint(player: Player, amount: Int) {
        val event = PlayerMagicPointEvent.Increase(player.plannersTemplate, amount)
        if (event.call()) {
            player.plannersTemplate.magicPoint += amount
        }
    }

    fun takeMagicPoint(player: Player, amount: Int) {
        val event = PlayerMagicPointEvent.Decrease(player.plannersTemplate, amount)
        if (event.call()) {
            player.plannersTemplate.magicPoint -= amount
        }
    }

    fun setMagicPoint(player: Player, to: Int) {
        val event = PlayerMagicPointEvent.Set(player.plannersTemplate, to)
        if (event.call()) {
            player.plannersTemplate.magicPoint = to
        }
    }

    fun resetMagicPoint(player: Player) {
        setMagicPoint(player, player.plannersTemplate.magicPointInUpperLimit)
    }

    fun addLevel(player: Player, amount: Int) {
        addLevel(player.plannersTemplate, amount)
    }

    fun setLevel(player: Player, to: Int) {
        setLevel(player.plannersTemplate, to)
    }

    fun addExperience(player: Player, amount: Int) {
        addExperience(player.plannersTemplate, amount)
    }

    fun setExperience(player: Player, value: Int) {
        setExperience(player.plannersTemplate, value)
    }

    fun takeExperience(player: Player, amount: Int) {
        takeExperience(player.plannersTemplate, amount)
    }

    fun addLevel(template: PlayerTemplate, amount: Int) {
        val event = PlayerLevelChangeEvent(template, template.level, template.level + amount)
        if (event.call()) {
            template.setLevel(template.level + amount)
        }
    }

    fun setLevel(template: PlayerTemplate, to: Int) {
        val event = PlayerLevelChangeEvent(template, template.level, to)
        if (event.call()) {
            template.setLevel(to)
        }
    }

    fun addExperience(template: PlayerTemplate, amount: Int) {
        val event = PlayerExperienceEvent.Increment(template, amount)
        if (event.call()) {
            template.addExperience(amount).thenAccept {
                PlayerExperienceEvent.Updated(template).call()
            }
        }
    }

    fun setExperience(template: PlayerTemplate, value: Int) {
        val event = PlayerExperienceEvent.Set(template, value)
        if (event.call()) {
            template.setExperience(value)
            PlayerExperienceEvent.Updated(template).call()
        }
    }

    fun takeExperience(template: PlayerTemplate, amount: Int) {
        val event = PlayerExperienceEvent.Decrement(template, amount)
        if (event.call()) {
            template.takeExperience(amount).thenAccept {
                PlayerExperienceEvent.Updated(template).call()
            }
        }
    }

    fun setSkillLevel(template: PlayerTemplate, skill: PlayerSkill, to: Int) {
        if (PlayerSkillEvent.LevelChange(template, skill, skill.level, to).call()) {
            skill.level = to
        }
    }

    fun setSkillBinding(template: PlayerTemplate, skill: PlayerSkill, binding: KeyBinding?) {
        PlayerSkillEvent.BindingChange(template, skill, binding).call()
        // 如果 binding 为 null 代表解绑
        if (binding == null) {
            skill.binding = null
        }
        // 解绑相同的快捷键的技能
        else {
            val registriedSkill = template.getRegisteredSkillOrNull(binding)
            if (registriedSkill != null) {
                setSkillBinding(template, registriedSkill, null)
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
                val template = Database.INSTANCE.getPlayerProfile(e.player)
//                 更新到玩家默认技能实例
                template.executeUpdatedDefaultSkill().thenAccept {
                    this@PlayerTemplateAPI[e.player.uniqueId] = template
                    PlayerProfileLoadedEvent(template).call()
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
            this@PlayerTemplateAPI.values().forEach { it.executeUpdatedDefaultSkill() }
        }
    }

    /**
     * 保存 metadata 资源
     */
    @Schedule(async = true, period = 20)
    private fun handleMetadataUpdater() {
        onlinePlayers.forEach { player ->
            val template = this.getOrNull(player.uniqueId) ?: return@forEach
            template.release().forEach { (key, data) ->
                Database.INSTANCE.updateMetadata(template, key, data)
            }
        }
    }


}
