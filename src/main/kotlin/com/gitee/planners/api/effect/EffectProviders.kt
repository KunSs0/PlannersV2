package com.gitee.planners.api.effect

import org.bukkit.Location

/**
 * Planners 类型化特效 Provider 的进程级注册入口。
 *
 * 同一时刻只允许一个所有者持有注册。不同实例不能覆盖已有 Provider，注销也只能由
 * 原注册所有者完成；缺少 Provider 时业务调用立即失败，不执行兼容或回退逻辑。
 */
object EffectProviders {

    private val registrationLock = Any()
    private var registration: Registration? = null

    /**
     * 注册唯一的特效 Provider。
     *
     * 同一所有者和同一 Provider 实例的重复注册视为幂等确认；其他覆盖请求直接失败。
     *
     * @param owner 负责 Provider 生命周期的所有者实例。
     * @param provider 外部插件提供的类型化特效实现。
     * @throws IllegalStateException 已存在其他注册实例时抛出。
     */
    @JvmStatic
    fun register(owner: Any, provider: EffectProvider) {
        synchronized(registrationLock) {
            val current = registration
            if (current == null) {
                registration = Registration(owner, provider)
                return
            }
            if (current.owner === owner && current.provider === provider) {
                return
            }
            throw IllegalStateException("A different Planners effect provider has already been registered")
        }
    }

    /**
     * 由原注册所有者注销当前特效 Provider。
     *
     * @param owner 发起注销的生命周期所有者实例。
     * @throws IllegalStateException 当前没有注册或调用方不是原所有者时抛出。
     */
    @JvmStatic
    fun unregister(owner: Any) {
        synchronized(registrationLock) {
            val current = registration
            if (current == null) {
                throw IllegalStateException("No Planners effect provider has been registered")
            }
            if (current.owner !== owner) {
                throw IllegalStateException("Only the registered Planners effect provider owner can unregister it")
            }
            registration = null
        }
    }

    /**
     * 使用当前唯一 Provider 播放世界位置特效。
     *
     * @param effectId 外部战斗插件定义的特效标识。
     * @param location 特效播放的世界坐标和朝向。
     * @param lifetimeTicks 特效持续的服务器 tick 数。
     * @return Provider 接受并发送播放请求时返回 true。
     * @throws IllegalArgumentException 参数不满足契约时抛出。
     * @throws IllegalStateException 尚未注册 Provider 时抛出。
     */
    @JvmStatic
    fun spawnAtLocation(effectId: String, location: Location, lifetimeTicks: Int): Boolean {
        if (effectId.isBlank()) {
            throw IllegalArgumentException("The Planners effect ID must not be blank")
        }
        if (lifetimeTicks < 0) {
            throw IllegalArgumentException("The Planners effect lifetime must not be negative")
        }
        val provider: EffectProvider
        synchronized(registrationLock) {
            val current = registration
            if (current == null) {
                throw IllegalStateException("No Planners effect provider has been registered")
            }
            provider = current.provider
        }
        return provider.spawnAtLocation(effectId, location, lifetimeTicks)
    }

    /**
     * 一次严格注册所绑定的所有者和 Provider 实例。
     *
     * @property owner 负责生命周期注销的原始所有者。
     * @property provider 当前唯一的特效实现。
     */
    private class Registration(
        val owner: Any,
        val provider: EffectProvider
    )
}
