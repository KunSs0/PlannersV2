package com.gitee.planners.api.directing

import java.util.concurrent.ConcurrentHashMap

/**
 * 指向会话创建器注册表。
 *
 * 此注册表只管理运行时创建器；配置解析仍由 [DirectingProviderRegistry] 负责。
 */
object DirectingSessionFactoryRegistry {

    private val factories = ConcurrentHashMap<String, DirectingSessionFactory>()

    /**
     * 注册一个运行时创建器。
     *
     * @param factory 待注册的创建器。
     * @throws IllegalArgumentException 类型非法或重复时抛出。
     */
    fun register(factory: DirectingSessionFactory) {
        val type = factory.type.trim()
        if (type.isEmpty()) {
            throw IllegalArgumentException("DirectingSessionFactory type 不能为空")
        }
        if (type != factory.type) {
            throw IllegalArgumentException("DirectingSessionFactory type 不能包含首尾空白: '${factory.type}'")
        }
        val previous = factories.putIfAbsent(type, factory)
        if (previous != null) {
            throw IllegalArgumentException("DirectingSessionFactory type 已注册: $type")
        }
    }

    /**
     * 获取指定类型的创建器。
     *
     * @param type provider 类型。
     * @return 对应创建器；未注册时返回 null。
     */
    fun getOrNull(type: String): DirectingSessionFactory? {
        return factories[type]
    }
}
