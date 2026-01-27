package com.gitee.planners.core.skill.script.impl

import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.skill.script.ScriptBukkitEventHolder
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import taboolib.platform.util.attacker

abstract class ScriptPlayerEvent<T: PlayerEvent> : ScriptBukkitEventHolder<T>() {

    override fun getSender(event: T): Target<*>? {
        return event.player.adaptTarget()
    }

    override fun handle(event: T, options: FluxonScriptOptions) {

    }

    object Join : ScriptPlayerEvent<PlayerJoinEvent>() {

        override val name = "player join"

        override val bind = PlayerJoinEvent::class.java

    }

    object Joined : ScriptBukkitEventHolder<PlayerProfileLoadedEvent>() {

        override val name = "player joined"

        override val bind = PlayerProfileLoadedEvent::class.java

        override fun getSender(event: PlayerProfileLoadedEvent): Target<*>? {
            return event.player.adaptTarget()
        }

        override fun handle(event: PlayerProfileLoadedEvent, options: FluxonScriptOptions) {

        }

    }

    object Quit : ScriptPlayerEvent<PlayerJoinEvent>() {

        override val name = "player quit"

        override val bind = PlayerJoinEvent::class.java

    }

    object Chat : ScriptPlayerEvent<AsyncPlayerChatEvent>() {

        override val name = "player chat"

        override val bind = AsyncPlayerChatEvent::class.java

    }

    // 玩家攻击实体
    object Attack: ScriptBukkitEventHolder<EntityDamageByEntityEvent>() {

        override val name: String = "player attack"

        override val bind: Class<EntityDamageByEntityEvent> = EntityDamageByEntityEvent::class.java

        override fun getSender(event: EntityDamageByEntityEvent): Target<*>? {
            val attacker = event.attacker
            if (attacker is Player) {
                return adaptTarget(attacker)
            }

            return null
        }

    }

    // 玩家被实体攻击
    class Damaged: ScriptBukkitEventHolder<EntityDamageByEntityEvent>() {

        override val name: String = "player damaged"

        override val bind: Class<EntityDamageByEntityEvent> = EntityDamageByEntityEvent::class.java

        override fun getSender(event: EntityDamageByEntityEvent): Target<*>? {
            val entity = event.entity
            if (entity is Player) {
                return adaptTarget(entity)
            }

            return null
        }

    }

    // 玩家切换奔跑
    object ToggleSprint: ScriptPlayerEvent<PlayerToggleSprintEvent>() {

        override val name: String = "player toggle sprint"

        override val bind: Class<PlayerToggleSprintEvent> = PlayerToggleSprintEvent::class.java

    }

    // 玩家切换潜行
    object ToggleSneak: ScriptPlayerEvent<PlayerToggleSprintEvent>() {

        override val name: String = "player toggle sneak"

        override val bind: Class<PlayerToggleSprintEvent> = PlayerToggleSprintEvent::class.java

    }

    // 玩家释放技能
    object CastSkill: ScriptBukkitEventHolder<PlayerSkillCastEvent.Post>() {

        override val name: String = "player cast skill"

        override val bind: Class<PlayerSkillCastEvent.Post> = PlayerSkillCastEvent.Post::class.java

        override fun getSender(event: PlayerSkillCastEvent.Post): Target<*>? {
            return adaptTarget(event.player)
        }

    }


}
