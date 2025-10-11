package com.gitee.planners.core.skill.script.state

import com.gitee.planners.api.event.entity.EntityStateEvent
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.skill.script.ScriptBukkitEventHolder
import org.bukkit.event.Event

abstract class ScriptEntityState<T : Event> : ScriptBukkitEventHolder<T>() {

    object Attach : ScriptEntityState<EntityStateEvent.Attach.Post>() {

        override val name: String = "state attach"

        override val bind: Class<EntityStateEvent.Attach.Post> = EntityStateEvent.Attach.Post::class.java

        override fun getSender(event: EntityStateEvent.Attach.Post): Target<*> {
            return event.entity
        }
    }

    object Detach : ScriptEntityState<EntityStateEvent.Detach.Pre>() {

        override val name: String = "state detach"

        override val bind: Class<EntityStateEvent.Detach.Pre> = EntityStateEvent.Detach.Pre::class.java

        override fun getSender(event: EntityStateEvent.Detach.Pre): Target<*> {
            return event.entity
        }
    }

    object Mount : ScriptEntityState<EntityStateEvent.Mount.Post>() {

        override val name: String = "state mount"

        override val bind: Class<EntityStateEvent.Mount.Post> = EntityStateEvent.Mount.Post::class.java

        override fun getSender(event: EntityStateEvent.Mount.Post): Target<*> {
            return event.entity
        }

    }

    object Close : ScriptEntityState<EntityStateEvent.Close.Pre>() {

        override val name: String = "state close"

        override val bind: Class<EntityStateEvent.Close.Pre> = EntityStateEvent.Close.Pre::class.java

        override fun getSender(event: EntityStateEvent.Close.Pre): Target<*> {
            return event.entity
        }

    }
    object End : ScriptEntityState<EntityStateEvent.End>() {

        override val name: String = "state end"

        override val bind: Class<EntityStateEvent.End> = EntityStateEvent.End::class.java

        override fun getSender(event: EntityStateEvent.End): Target<*> {
            return event.entity
        }
    }
}
