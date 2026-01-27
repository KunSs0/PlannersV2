package com.gitee.planners.api.event.entity

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.State
import taboolib.platform.type.BukkitProxyEvent

class EntityStateEvent {

    /**
     * 状态附加到实体时回调。
     */
    abstract class Attach(val entity: ProxyTarget<*>, val state: State) : BukkitProxyEvent() {

        class Pre(entity: ProxyTarget<*>, state: State) : Attach(entity, state)

        class Post(entity: ProxyTarget<*>, state: State) : Attach(entity, state) {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    /**
     * 状态从实体移除时回调。
     */
    abstract class Detach(val entity: ProxyTarget<*>, val state: State) : BukkitProxyEvent() {

        class Pre(entity: ProxyTarget<*>, state: State) : Detach(entity, state)

        class Post(entity: ProxyTarget<*>, state: State) : Detach(entity, state) {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    /**
     * 状态首层挂载时额外回调。
     */
    abstract class Mount(val entity: ProxyTarget<*>, val state: State) : BukkitProxyEvent() {

        class Pre(entity: ProxyTarget<*>, state: State) : Mount(entity, state)

        class Post(entity: ProxyTarget<*>, state: State) : Mount(entity, state) {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    /**
     * 状态全部移除后额外回调。
     */
    abstract class Close(val entity: ProxyTarget<*>, val state: State) : BukkitProxyEvent() {

        class Pre(entity: ProxyTarget<*>, state: State) : Close(entity, state)

        class Post(entity: ProxyTarget<*>, state: State) : Close(entity, state) {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    /**
     * 状态自然到期时回调。
     */
    class End(val entity: ProxyTarget<*>, val state: State) : BukkitProxyEvent() {
        override val allowCancelled: Boolean
            get() = false
    }
}
