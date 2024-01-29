package com.gitee.planners.module.event.impl

import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.event.ScriptBukkitEventWrapped
import com.gitee.planners.module.event.ScriptEventWrapped
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.module.kether.ScriptContext

abstract class ScriptPlayerEvent<T: PlayerEvent> : ScriptBukkitEventWrapped<T> {

    override fun getSender(event: T): Target<*>? {
        return event.player.adaptTarget()
    }

    override fun handle(event: T, ctx: ScriptContext) {

    }

    object Join : ScriptPlayerEvent<PlayerJoinEvent>() {

        override val name = "join"

        override val bind = PlayerJoinEvent::class.java

    }

    object Joined : ScriptBukkitEventWrapped<PlayerProfileLoadedEvent> {

        override val name = "joined"

        override val bind = PlayerProfileLoadedEvent::class.java

        override fun getSender(event: PlayerProfileLoadedEvent): Target<*>? {
            return event.player.adaptTarget()
        }

        override fun handle(event: PlayerProfileLoadedEvent, ctx: ScriptContext) {

        }

    }

    object Quit : ScriptPlayerEvent<PlayerJoinEvent>() {

        override val name = "quit"

        override val bind = PlayerJoinEvent::class.java

    }

    object Chat : ScriptPlayerEvent<AsyncPlayerChatEvent>() {

        override val name = "chat"

        override val bind = AsyncPlayerChatEvent::class.java

    }


}
