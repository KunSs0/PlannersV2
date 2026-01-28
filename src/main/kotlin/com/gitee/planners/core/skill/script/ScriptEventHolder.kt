package com.gitee.planners.core.skill.script

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptOptions

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
    fun getSender(event: T) : ProxyTarget<*>?

    /**
     * 处理事件
     *
     * @param event 事件对象
     * @param options 脚本选项
     */
    fun handle(event: T, options: FluxonScriptOptions)

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
    fun getCallback(id: String): ScriptCallback<T>?

    /**
     * 注册监听器
     *
     * @param callback 监听器
     */
    fun register(callback: ScriptCallback<T>)

    /**
     * 注销监听器
     *
     * @param callback 监听器
     */
    fun unregister(callback: ScriptCallback<T>)
}
