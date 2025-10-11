package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.State
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor

class TargetStateHolder(
    val state: State,
    duration: Long,
    private val cb: Runnable?
) : Metadata.Unsaved {

    var duration: Long = duration
        private set

    var end: Long = System.currentTimeMillis() + duration * 50
        private set

    var layer: Int = 1
        private set

    var isValid: Boolean = true
        private set

    val isExpired: Boolean
        get() = !isValid || System.currentTimeMillis() >= end

    private var task: PlatformExecutor.PlatformTask? = null

    fun init() {
        schedule(duration)
    }

    fun refresh(duration: Long) {
        if (!isValid) {
            return
        }
        schedule(duration)
    }

    fun incrementLayer(maxLayer: Int): Boolean {
        if (layer >= maxLayer) {
            return false
        }
        layer++
        return true
    }

    fun decrementLayer(amount: Int): Int {
        if (amount <= 0) {
            return layer
        }
        if (amount >= layer) {
            layer = 0
        } else {
            layer -= amount
        }
        return layer
    }

    fun close() {
        task?.cancel()
        task = null
        isValid = false
        layer = 0
    }

    private fun schedule(delay: Long) {
        task?.cancel()
        isValid = true
        duration = delay
        end = System.currentTimeMillis() + delay * 50
        task = submit(delay = delay, async = true) {
            isValid = false
            cb?.run()
        }
    }

    companion object {

        fun create(state: State, duration: Long, cb: Runnable?): TargetStateHolder {
            return TargetStateHolder(state, duration, cb)
        }

        fun parse(metadata: Metadata?): TargetStateHolder? {
            if (metadata == null) {
                return null
            }
            return metadata.any() as TargetStateHolder
        }
    }
}
