package com.gitee.planners.api

import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.*
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.api.template.ProfileOperatorImpl
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPointInUpperLimit
import com.gitee.planners.util.builtin.MutableRegistryInMap
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submitAsync
import taboolib.common5.util.startsWithAny
import taboolib.platform.util.onlinePlayers
import java.util.UUID
import java.util.concurrent.CompletableFuture

object PlayerTemplateAPI : MutableRegistryInMap<UUID, PlayerTemplate>() {

    val OPERATOR = ProfileOperatorImpl()

    val Player.plannersLoaded: Boolean
        get() = containsKey(uniqueId)

    val Player.plannersTemplate: PlayerTemplate
        get() = getOrNull(this.uniqueId) ?: error("Player $name unloaded.")

    /**
     * 设置玩家角色
     *
     * @param player 玩家
     * @param route 路线
     *
     * @return CompletableFuture<PlayerRoute>
     */
    fun setPlayerRoute(player: Player, route: ImmutableRoute): CompletableFuture<PlayerRoute> {
        val template = player.plannersTemplate
        if (PlayerSetRouteEvent.Pre(template, route).call()) {
            return PlayerTemplateAPI.OPERATOR.createPlayerRoute(template, route).thenApply {
                template.route = it
                PlayerSetRouteEvent.Post(template, it).call()

                it
            }
        }

        return CompletableFuture.completedFuture(null)
    }

    /**
     * 添加魔法点数
     *
     * @param player 玩家
     * @param amount 增加的魔法点数
     */
    fun addMagicPoint(player: Player, amount: Int) {
        val event = PlayerMagicPointEvent.Increase(player.plannersTemplate, amount)
        if (event.call()) {
            player.plannersTemplate.magicPoint += amount
        }
    }

    /**
     * 减少魔法点数
     *
     * @param player 玩家
     * @param amount 减少的魔法点数
     */
    fun takeMagicPoint(player: Player, amount: Int) {
        val event = PlayerMagicPointEvent.Decrease(player.plannersTemplate, amount)
        if (event.call()) {
            player.plannersTemplate.magicPoint -= amount
        }
    }

    /**
     * 设置魔法点数
     *
     * @param player 玩家
     * @param to 目标魔法点数
     */
    fun setMagicPoint(player: Player, to: Int) {
        val event = PlayerMagicPointEvent.Set(player.plannersTemplate, to)
        if (event.call()) {
            player.plannersTemplate.magicPoint = to
        }
    }

    /**
     * 重置魔法点数
     *
     * @param player 玩家
     */
    fun resetMagicPoint(player: Player) {
        setMagicPoint(player, player.plannersTemplate.magicPointInUpperLimit)
    }

    /**
     * 添加玩家等级
     *
     * @param player 玩家
     * @param amount 增加的等级
     */
    fun addLevel(player: Player, amount: Int) {
        addLevel(player.plannersTemplate, amount)
    }

    /**
     * 设置玩家等级
     *
     * @param player 玩家
     * @param to 目标等级
     */
    fun setLevel(player: Player, to: Int) {
        setLevel(player.plannersTemplate, to)
    }

    /**
     * 添加玩家经验
     *
     * @param player 玩家
     * @param amount 增加的经验值
     */
    fun addExperience(player: Player, amount: Int) {
        addExperience(player.plannersTemplate, amount)
    }

    /**
     * 设置玩家经验
     *
     * @param player 玩家
     * @param value 目标经验值
     */
    fun setExperience(player: Player, value: Int) {
        setExperience(player.plannersTemplate, value)
    }

    /**
     * 减少玩家经验
     *
     * @param player 玩家
     * @param amount 减少的经验值
     */
    fun takeExperience(player: Player, amount: Int) {
        takeExperience(player.plannersTemplate, amount)
    }

    /**
     * 添加玩家等级
     *
     * @param template 玩家模板
     * @param amount 增加的等级
     */
    fun addLevel(template: PlayerTemplate, amount: Int) {
        val event = PlayerLevelChangeEvent(template, template.level, template.level + amount)
        if (event.call()) {
            template.setLevel(template.level + amount)
        }
    }

    /**
     * 设置玩家等级
     *
     * @param template 玩家模板
     * @param to 目标等级
     */
    fun setLevel(template: PlayerTemplate, to: Int) {
        val event = PlayerLevelChangeEvent(template, template.level, to)
        if (event.call()) {
            template.setLevel(to)
        }
    }

    /**
     * 添加玩家经验
     *
     * @param template 玩家模板
     * @param amount 增加的经验值
     */
    fun addExperience(template: PlayerTemplate, amount: Int) {
        val event = PlayerExperienceEvent.Increment(template, amount)
        if (event.call()) {
            val level0 = template.level
            template.addExperience(amount).thenAccept {
                if (level0 != template.level) {
                    PlayerLevelChangeEvent(template, level0, template.level).call()
                }
                PlayerExperienceEvent.Updated(template).call()
            }
        }
    }

    /**
     * 设置玩家经验
     *
     * @param template 玩家模板
     * @param value 目标经验值
     */
    fun setExperience(template: PlayerTemplate, value: Int) {
        val event = PlayerExperienceEvent.Set(template, value)
        if (event.call()) {
            template.setExperience(value)
            PlayerExperienceEvent.Updated(template).call()
        }
    }

    /**
     * 减少玩家经验
     *
     * @param template 玩家模板
     * @param amount 减少的经验值
     */
    fun takeExperience(template: PlayerTemplate, amount: Int) {
        val event = PlayerExperienceEvent.Decrement(template, amount)
        if (event.call()) {
            val level0 = template.level
            template.takeExperience(amount).thenAccept {
                if (level0 != template.level) {
                    PlayerLevelChangeEvent(template, level0, template.level).call()
                }
                PlayerExperienceEvent.Updated(template).call()
            }
        }
    }

    /**
     * 设置技能等级
     *
     * @param template 玩家模板
     * @param skill 技能
     * @param to 目标等级
     */
    fun setSkillLevel(template: PlayerTemplate, skill: PlayerSkill, to: Int) {
        if (PlayerSkillEvent.LevelChange(template, skill, skill.level, to).call()) {
            skill.level = to
        }
    }

    /**
     * 设置技能绑定
     *
     * @param template 玩家模板
     * @param skill 技能
     * @param binding 快捷键绑定
     */
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
                    info("Loaded player profile for ${e.player.name} (${e.player.uniqueId})")
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
            template.release().forEach inner@{ (key, data) ->
                // 内置字段不保存
                if (key.startsWith("__")) {
                    return@inner
                }
                Database.INSTANCE.updateMetadata(template, key, data)
            }
        }
    }


}
