package com.gitee.planners.api.directing

import java.util.concurrent.ConcurrentHashMap

/**
 * 指向性技能 provider 注册表。
 *
 * 该对象是 provider 注册、查询和重复校验的唯一拥有者。
 */
object DirectingProviderRegistry {

    private val providers = ConcurrentHashMap<String, DirectingProvider>()

    /**
     * 注册一个 provider。
     *
     * @param provider 待注册的 provider。
     * @throws IllegalArgumentException 当 type 为空或已被其他 provider 占用时抛出。
     */
    fun register(provider: DirectingProvider) {
        val type = provider.type.trim()
        if (type.isEmpty()) {
            throw IllegalArgumentException("DirectingProvider type 不能为空")
        }
        if (type != provider.type) {
            throw IllegalArgumentException("DirectingProvider type 不能包含首尾空白: '${provider.type}'")
        }

        val previous = providers.putIfAbsent(type, provider)
        if (previous != null) {
            throw IllegalArgumentException("DirectingProvider type 已注册: $type")
        }
    }

    /**
     * 按 type 获取 provider。
     *
     * @param type provider 类型标识。
     * @return 匹配的 provider；未注册时返回 null。
     */
    fun getOrNull(type: String): DirectingProvider? {
        return providers[type]
    }
}
