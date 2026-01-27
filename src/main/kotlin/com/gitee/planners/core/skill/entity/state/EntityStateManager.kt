package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.api.event.entity.EntityStateEvent
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetContainerization
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.core.config.State
import com.gitee.planners.core.config.State.Companion.path
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

/**
 * 状态管理器内部实现
 */
object EntityStateManager {

    fun has(target: TargetEntity<*>, state: State): Boolean {
        if (state.isStatic) {
            return true
        }
        val container = target as? TargetContainerization ?: return false
        val holder = TargetStateHolder.parse(container.getMetadata(state.path()))
        return holder?.let { it.isValid && it.layer > 0 } ?: false
    }

    fun isExpired(target: TargetEntity<*>, state: State): Boolean {
        val container = target as? TargetContainerization ?: return true
        val holder = TargetStateHolder.parse(container.getMetadata(state.path()))
        return holder?.isExpired ?: true
    }

    fun attach(target: TargetEntity<*>, state: State, duration: Long, refresh: Boolean) {
        if (duration <= 0) {
            warning("状态 ${state.id} 的持续时间必须大于 0")
            return
        }

        val container = target as? TargetContainerization ?: return
        val bukkitTarget = target as? TargetBukkitEntity ?: return

        val key = state.path()
        val holder = TargetStateHolder.parse(container.getMetadata(key))
        val hasValidHolder = holder != null && holder.isValid && holder.layer > 0

        val maxLayer = state.maxLayer.coerceAtLeast(1)
        if (hasValidHolder && holder!!.layer >= maxLayer && !refresh) {
            return
        }

        val isFirstLayer = !hasValidHolder
        if (isFirstLayer && !EntityStateEvent.Mount.Pre(bukkitTarget, state).call()) {
            return
        }
        if (!EntityStateEvent.Attach.Pre(bukkitTarget, state).call()) {
            return
        }

        if (isFirstLayer) {
            holder?.close()
            val newHolder = TargetStateHolder.create(state, duration) {
                endState(target, state)
            }
            newHolder.init()
            container.setMetadata(key, metadataValue(newHolder))
            EntityStateEvent.Attach.Post(bukkitTarget, state).call()
            EntityStateEvent.Mount.Post(bukkitTarget, state).call()
            return
        }

        val existingHolder = holder!!
        val incremented = existingHolder.incrementLayer(maxLayer)
        if (refresh) {
            existingHolder.refresh(duration)
        }
        EntityStateEvent.Attach.Post(bukkitTarget, state).call()

        if (!incremented && !refresh && existingHolder.layer >= maxLayer) {
            return
        }
    }

    fun detach(target: TargetEntity<*>, state: State, layer: Int) {
        if (state.isStatic) {
            return
        }

        val container = target as? TargetContainerization ?: return
        val bukkitTarget = target as? TargetBukkitEntity ?: return

        val key = state.path()
        val holder = TargetStateHolder.parse(container.getMetadata(key)) ?: return

        if (holder.layer <= 0) {
            container.setMetadata(key, metadataValue(null))
            return
        }

        val removal = if (layer >= 999) holder.layer.coerceAtLeast(1) else layer.coerceAtLeast(1)
        val finalRemoval = removal >= holder.layer

        if (!EntityStateEvent.Detach.Pre(bukkitTarget, state).call()) {
            return
        }

        if (!finalRemoval) {
            holder.decrementLayer(removal)
            EntityStateEvent.Detach.Post(bukkitTarget, state).call()
            return
        }

        if (!performFullRemoval(bukkitTarget, state, holder, key, container)) {
            return
        }
        EntityStateEvent.Detach.Post(bukkitTarget, state).call()
        EntityStateEvent.Close.Post(bukkitTarget, state).call()
    }

    fun remove(target: TargetEntity<*>, state: State) {
        if (state.isStatic) {
            return
        }

        val container = target as? TargetContainerization ?: return
        val bukkitTarget = target as? TargetBukkitEntity ?: return

        val key = state.path()
        val holder = TargetStateHolder.parse(container.getMetadata(key)) ?: return

        if (holder.layer <= 0) {
            container.setMetadata(key, metadataValue(null))
            return
        }

        if (!EntityStateEvent.Detach.Pre(bukkitTarget, state).call()) {
            return
        }
        if (!performFullRemoval(bukkitTarget, state, holder, key, container)) {
            return
        }
        EntityStateEvent.Detach.Post(bukkitTarget, state).call()
        EntityStateEvent.Close.Post(bukkitTarget, state).call()
    }

    private fun endState(target: TargetEntity<*>, state: State) {
        info("State ${state.id} timer task ended")
        val bukkitTarget = target as? TargetBukkitEntity ?: return
        if (EntityStateEvent.End(bukkitTarget, state).call()) {
            remove(target, state)
        }
    }

    private fun performFullRemoval(
        bukkitTarget: TargetBukkitEntity,
        state: State,
        holder: TargetStateHolder,
        key: String,
        container: TargetContainerization
    ): Boolean {
        if (!EntityStateEvent.Close.Pre(bukkitTarget, state).call()) {
            return false
        }
        holder.close()
        container.setMetadata(key, metadataValue(null))
        return true
    }
}
