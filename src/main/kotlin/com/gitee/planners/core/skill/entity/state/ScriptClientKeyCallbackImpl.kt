package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.event.ProxyClientKeyEvents
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptEventHolder

/**
 * 客户端按键脚本回调
 *
 * 支持：
 *   listen: keydown       -> 任意按键按下
 *   listen: keydown R     -> 仅按下 R 键
 *   listen: keyup         -> 任意按键抬起
 *   listen: keyup R       -> 仅抬起 R 键
 */
open class ScriptClientKeyCallbackImpl<T>(
    state: State,
    trigger: State.Trigger,
    /** 事件名，"keydown" 或 "keyup" */
    private val eventName: String,
) : ScriptCallbackImpl<T>(state, trigger) {

    /**
     * 期望匹配的按键，若为空则表示任意按键。
     */
    private val key: String? = trigger.listen
        .replace(eventName, "", ignoreCase = true)
        .trim()
        .takeIf { it.isNotEmpty() }

    @Suppress("UNCHECKED_CAST")
    override fun call(sender: Target<*>, event: T, holder: ScriptEventHolder<T>) {
        if (key != null) {
            // 从事件对象中提取按键名称
            val eventKey = when (event) {
                is ProxyClientKeyEvents.Down -> event.key
                is ProxyClientKeyEvents.Up -> event.key
                else -> null
            }

            if (eventKey == null || !eventKey.equals(key, ignoreCase = true)) {
                // 按键不匹配，直接丢弃
                return
            }
        }

        // 按键匹配后走状态校验 + 脚本执行
        super.call(sender, event, holder)
    }
}
