package com.gitee.planners.api.common.registry

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration

fun <K,V> defaultRegistry() = DefaultRegistry<K,V>()

fun <T : Unique> deepTrackRegistry(name: String, attaches: List<String> = emptyList(), block: Configuration.() -> T): DeepTrackConfigurationRegistry<T> {
    return object : DeepTrackConfigurationRegistry<T>(name, attaches) {

        override fun invokeInstance(config: Configuration): T {
            return block(config)
        }

    }
}

fun <T : Unique> singletonRegistry(path: String, block: ConfigurationSection.() -> T): Registry<String, T> {
    return object : SingletonConfigurationRegistry<T>(path) {

        override fun invokeInstance(config: ConfigurationSection): T {
            return block(config)
        }

    }
}

/** 配置响应式注册机 */
fun <T : Unique> singletonResponsiveRegistry(path: String, block: ConfigurationSection.() -> T): SingletonConfigurationRegistry<T> {
    return object : SingletonConfigurationRegistry<T>(path) {

        override val responsive = true

        override fun invokeInstance(config: ConfigurationSection): T {
            return block(config)
        }
    }
}

/** 配置响应式 */
fun <T> singletonWithInferRegistry(path: String, block: ConfigurationSection.() -> T): SingletonConfigurationWithInferRegistry<T> where T : Unique, T : SingletonConfigurationWithInferRegistry.Infer {
    return object : SingletonConfigurationWithInferRegistry<T>(path) {

        override fun invokeInstance(config: ConfigurationSection): T {
            return block(config)
        }

    }
}

fun <K, V> mutableRegistry(prev: Map<K, V> = emptyMap()): MutableRegistry<K, V> {
    return MutableRegistry(prev)
}
