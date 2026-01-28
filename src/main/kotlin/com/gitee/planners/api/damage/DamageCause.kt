package com.gitee.planners.api.damage

import org.bukkit.event.entity.EntityDamageEvent
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * 自定义伤害原因，支持 Bukkit 枚举和自定义扩展
 */
sealed interface DamageCause {

    val name: String

    /** Bukkit 原生伤害类型 */
    class Bukkit(val cause: EntityDamageEvent.DamageCause) : DamageCause {
        override val name: String = cause.name
        override fun toString(): String = "DamageCause.Bukkit($name)"
        override fun hashCode(): Int = cause.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Bukkit) return false
            return cause == other.cause
        }
    }

    /** 自定义伤害类型 */
    class Custom(override val name: String) : DamageCause {
        override fun toString(): String = "DamageCause.Custom($name)"
        override fun hashCode(): Int = name.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Custom) return false
            return name == other.name
        }
    }

    companion object {

        @Config
        private lateinit var config: Configuration

        /** 已注册的自定义伤害类型 */
        private val registry = mutableMapOf<String, Custom>()

        /** 注册自定义伤害类型 */
        fun register(name: String): Custom {
            val key = name.uppercase()
            return registry.getOrPut(key) { Custom(key) }
        }

        /** 获取所有已注册的自定义类型 */
        fun registered(): Collection<Custom> = registry.values

        /** 检查自定义类型是否已注册 */
        fun isRegistered(name: String): Boolean = registry.containsKey(name.uppercase())

        /** 从配置加载自定义伤害类型 */
        fun reload() {
            registry.clear()
            config.getStringList("settings.damage-causes").forEach { name ->
                register(name)
            }
        }

        /**
         * 根据名称获取伤害原因
         * @throws IllegalArgumentException 如果自定义类型未在配置中定义
         */
        fun of(name: String): DamageCause {
            val key = name.uppercase()
            // 优先匹配 Bukkit 枚举
            return try {
                Bukkit(EntityDamageEvent.DamageCause.valueOf(key))
            } catch (_: IllegalArgumentException) {
                // 必须是已注册的自定义类型
                registry[key] ?: throw IllegalArgumentException(
                    "Unknown damage cause '$name'. Custom causes must be defined in config.yml under 'settings.damage-causes'"
                )
            }
        }

        /**
         * 根据名称获取伤害原因，未注册返回 null
         */
        fun ofOrNull(name: String): DamageCause? {
            val key = name.uppercase()
            return try {
                Bukkit(EntityDamageEvent.DamageCause.valueOf(key))
            } catch (_: IllegalArgumentException) {
                registry[key]
            }
        }

        /** 包装 Bukkit 伤害原因 */
        fun of(cause: EntityDamageEvent.DamageCause): Bukkit {
            return Bukkit(cause)
        }
    }
}
