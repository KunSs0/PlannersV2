package com.gitee.planners.core.skill.script

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.job.target.Target
import taboolib.module.kether.ScriptContext

interface ScriptEventHolder<T> {

    val name: String

    val bind: Class<T>

    /**
     * 获取触发者
     *
     * @param event 事件对象
     *
     * @return 触发者
     */
    fun getSender(event: T) : Target<*>?

    /**
     * 处理事件
     *
     * @param event 事件对象
     * @param ctx 脚本上下文
     */
    fun handle(event: T, ctx: ScriptContext)

    /**
     * 获取修饰符
     *
     * @param event 事件对象
     *
     * @return 修饰符
     */
    fun getModifier(event: T) : Animated? {
        return null
    }

    /**
     * 初始化
     */
    fun init()

    /**
     * 卸载
     */
    fun unload()

    /**
     * 获取回调
     *
     * @param id 脚本id
     */
    fun getCallback(id: String): ScriptCallback?

    /**
     * 注册监听器
     *
     * @param callback 监听器
     */
    fun register(callback: ScriptCallback)

    /**
     * 注销监听器
     *
     * @param callback 监听器
     */
    fun unregister(callback: ScriptCallback)
}
