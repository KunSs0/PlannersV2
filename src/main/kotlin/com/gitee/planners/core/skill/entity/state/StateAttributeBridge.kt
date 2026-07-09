package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.event.entity.EntityStateEvent
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.State
import com.gitee.planners.core.config.State.Companion.path
import com.gitee.planners.module.compat.attribute.AttributeDriver
import taboolib.common.platform.event.SubscribeEvent

/**
 * 状态属性到外部属性驱动的生命周期桥接。
 */
object StateAttributeBridge {

    /**
     * 状态附加后同步属性源。
     *
     * @param event 状态附加后事件。
     */
    @SubscribeEvent
    fun onStateAttach(event: EntityStateEvent.Attach.Post) {
        sync(event.entity, event.state)
    }

    /**
     * 状态层数移除后同步属性源。
     *
     * @param event 状态移除后事件。
     */
    @SubscribeEvent
    fun onStateDetach(event: EntityStateEvent.Detach.Post) {
        sync(event.entity, event.state)
    }

    /**
     * 状态完全关闭后清理属性源。
     *
     * @param event 状态完全关闭后事件。
     */
    @SubscribeEvent
    fun onStateClose(event: EntityStateEvent.Close.Post) {
        AttributeDriver.remove(event.entity, event.state.path())
    }

    /**
     * 按状态当前层数重建属性源。
     *
     * @param target 状态目标。
     * @param state 状态配置。
     */
    private fun sync(target: ProxyTarget<*>, state: State) {
        AttributeDriver.remove(target, state.path())
        if (state.attribute.isEmpty()) {
            return
        }

        val stateHolder = getStateHolder(target, state)
        if (stateHolder == null || !stateHolder.isValid || stateHolder.layer <= 0) {
            return
        }

        val tokens = buildLayeredTokens(state.attribute, stateHolder.layer)
        if (tokens.isEmpty()) {
            return
        }

        AttributeDriver.set(target, state.path(), tokens, -1)
    }

    /**
     * 读取状态持有器。
     *
     * @param target 状态目标。
     * @param state 状态配置。
     * @return 状态持有器。
     */
    private fun getStateHolder(target: ProxyTarget<*>, state: State): TargetStateHolder? {
        if (target !is ProxyTarget.Containerization) {
            return null
        }
        val metadata = target.getMetadata(state.path())
        if (metadata == null) {
            return null
        }
        return TargetStateHolder.parse(metadata)
    }

    /**
     * 按状态层数重复属性 token。
     *
     * @param attribute 状态 attribute 配置。
     * @param layer 当前状态层数。
     * @return 重复后的属性 token。
     */
    private fun buildLayeredTokens(attribute: List<String>, layer: Int): List<String> {
        val result = ArrayList<String>()
        if (layer <= 0) {
            return result
        }
        var index = 0
        while (index < layer) {
            for (token in attribute) {
                result.add(token)
            }
            index++
        }
        return result
    }
}
