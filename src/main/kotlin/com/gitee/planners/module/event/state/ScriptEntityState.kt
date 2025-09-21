package com.gitee.planners.module.event.state

import com.gitee.planners.api.event.entity.EntityStateEvent
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.module.event.ScriptBukkitEventHolder
import org.bukkit.event.Event

abstract class ScriptEntityState<T : Event> : ScriptBukkitEventHolder<T>() {


    object Attach : ScriptEntityState<EntityStateEvent.Attach.Post>() {

        override val name: String = "state-attach"

        override val bind: Class<EntityStateEvent.Attach.Post> = EntityStateEvent.Attach.Post::class.java

        override fun getSender(event: EntityStateEvent.Attach.Post): Target<*> {
            return event.entity
        }

    }

    object Detach : ScriptEntityState<EntityStateEvent.Detach.Post>() {

        override val name: String = "state-detach"

        override val bind: Class<EntityStateEvent.Detach.Post> = EntityStateEvent.Detach.Post::class.java

        override fun getSender(event: EntityStateEvent.Detach.Post): Target<*> {
            return event.entity
        }

    }

}