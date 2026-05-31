package com.gitee.planners.api.damage

import com.gitee.planners.Planners
import org.bukkit.event.entity.EntityDamageEvent

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

        /**
         * 根据名称获取伤害原因。
         * 优先匹配 Bukkit 枚举，否则从 config.yml → settings.damage-causes 查找。
         * @throws IllegalArgumentException 如果未定义
         */
        fun of(name: String): DamageCause {
            val key = name.uppercase()
            try {
                return Bukkit(EntityDamageEvent.DamageCause.valueOf(key))
            } catch (_: IllegalArgumentException) {
                if (hasCustom(name)) {
                    return Custom(key)
                }
                throw IllegalArgumentException(
                    "Unknown damage cause '$name'. Custom causes must be defined in config.yml under 'settings.damage-causes'"
                )
            }
        }

        /**
         * 根据名称获取伤害原因，未定义返回 null
         */
        fun ofOrNull(name: String): DamageCause? {
            val key = name.uppercase()
            try {
                return Bukkit(EntityDamageEvent.DamageCause.valueOf(key))
            } catch (_: IllegalArgumentException) {
                if (hasCustom(name)) {
                    return Custom(key)
                }
                return null
            }
        }

        /** 包装 Bukkit 伤害原因 */
        fun of(cause: EntityDamageEvent.DamageCause): Bukkit {
            return Bukkit(cause)
        }

        private fun hasCustom(name: String): Boolean {
            val causes = Planners.damageCauses.get()
            return causes.any { it.equals(name, ignoreCase = true) }
        }
    }
}
