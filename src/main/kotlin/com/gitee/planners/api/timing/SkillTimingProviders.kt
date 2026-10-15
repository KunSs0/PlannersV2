package com.gitee.planners.api.timing

import java.util.concurrent.ConcurrentHashMap

/**
 * 技能时间Provider的严格注册表，没有缺失回退或隐式实现。
 */
object SkillTimingProviders {
    private val registrations = ConcurrentHashMap<String, Registration>()

    /**
     * 注册配置可引用的时间Provider。
     *
     * @param id 唯一Provider ID；unmodified为显式原时间模式的保留名称。
     * @param owner 生命周期所有者。
     * @param provider 属性收益实现。
     * @throws IllegalArgumentException ID为空或为保留名称时抛出。
     * @throws IllegalStateException 已有其他所有者或实现时抛出。
     */
    @JvmStatic
    fun register(id: String, owner: Any, provider: SkillTimingProvider) {
        // 不允许不同插件覆盖注册，避免同一配置在运行时切换计算来源。
        require(id.isNotBlank() && id != "unmodified") { "技能时间Provider ID不能为空或为unmodified。" }
        val previous = registrations.putIfAbsent(id, Registration(owner, provider))
        check(previous == null || previous.owner === owner && previous.provider === provider) { "技能时间Provider已注册：$id" }
    }

    /**
     * 按原所有者注销Provider。
     *
     * @param id 已注册的Provider ID。
     * @param owner 原生命周期所有者。
     * @throws IllegalStateException 未注册或所有者不一致时抛出。
     */
    @JvmStatic
    fun unregister(id: String, owner: Any) {
        // 原子校验所有权，防止其他插件移除仍在使用的实现。
        registrations.compute(id) { _, current ->
            check(current != null && current.owner === owner) { "不能注销不属于当前所有者的技能时间Provider：$id" }
            null
        }
    }

    /**
     * 获取配置指定的唯一实现。
     *
     * @param id 配置中的Provider ID。
     * @return 对应的时间收益实现。
     * @throws IllegalStateException 尚未注册时抛出，不采用原时间兜底。
     */
    fun requireProvider(id: String): SkillTimingProvider {
        // 明确失败比静默忽略急速更容易定位插件加载和卸载问题。
        val registration = registrations[id]
        check(registration != null) { "settings.skill.timing.provider指定的实现未注册：$id" }
        return registration.provider
    }

    /**
     * 保存一个Provider及其生命周期所有权。
     *
     * @property owner 注册所有者。
     * @property provider 实际属性实现。
     */
    private class Registration(val owner: Any, val provider: SkillTimingProvider)
}
