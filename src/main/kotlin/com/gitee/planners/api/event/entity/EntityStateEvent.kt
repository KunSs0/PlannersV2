package com.gitee.planners.api.event.entity

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.State
import taboolib.platform.type.BukkitProxyEvent

class EntityStateEvent {

    /**
     * 实体添加状态时触发
     *
     * @param entity 实体
     * @param state 状态
     */
    abstract class Attach(val entity: Target<*>, val state: State): BukkitProxyEvent() {

        class Pre(entity: Target<*>, state: State) : Attach(entity, state)

        class Post(entity: Target<*>, state: State) : Attach(entity, state) {

            override val allowCancelled: Boolean
                get() = false

        }

    }

    /**
     * 实体移除状态时触发
     *
     * @param entity 实体
     * @param state 状态
     */
    abstract class Detach(val entity: Target<*>, val state: State): BukkitProxyEvent() {

        class Pre(entity: Target<*>, state: State) : Detach(entity, state)

        class Post(entity: Target<*>, state: State) : Detach(entity, state) {

            override val allowCancelled: Boolean
                get() = false

        }

    }

}