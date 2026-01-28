package com.gitee.planners.api.damage

import org.bukkit.entity.LivingEntity
import taboolib.platform.util.setMeta

/**
 * 伤害代理类，封装伤害上下文
 */
class ProxyDamage private constructor(
    val source: LivingEntity?,
    val target: LivingEntity,
    val baseDamage: Double,
    val cause: DamageCause,
    val metadata: Map<String, Any>
) {
    /** 最终伤害（可被修改） */
    var finalDamage: Double = baseDamage

    /** 是否被取消 */
    var isCancelled: Boolean = false

    /** 执行伤害 */
    fun execute(): DamageResult {
        if (isCancelled) return DamageResult.CANCELLED
        if (!target.isValid) return DamageResult.CANCELLED

        // 设置击杀者标记
        if (source != null && source != target && target.health <= finalDamage) {
            target.setMeta("@killer", source)
        }

        // 执行伤害
        if (source != null) {
            target.damage(finalDamage, source)
        } else {
            target.damage(finalDamage)
        }

        return DamageResult.SUCCESS
    }

    override fun toString(): String {
        return "ProxyDamage(source=$source, target=$target, damage=$finalDamage, cause=$cause)"
    }

    companion object {
        fun builder(): Builder = Builder()

        /** 快捷构建：无来源伤害 */
        fun of(target: LivingEntity, damage: Double): ProxyDamage {
            return Builder()
                .target(target)
                .damage(damage)
                .build()
        }

        /** 快捷构建：有来源伤害 */
        fun of(source: LivingEntity?, target: LivingEntity, damage: Double): ProxyDamage {
            return Builder()
                .source(source)
                .target(target)
                .damage(damage)
                .build()
        }

        /** 快捷构建：有来源和原因 */
        fun of(source: LivingEntity?, target: LivingEntity, damage: Double, cause: DamageCause): ProxyDamage {
            return Builder()
                .source(source)
                .target(target)
                .damage(damage)
                .cause(cause)
                .build()
        }
    }

    class Builder {
        private var source: LivingEntity? = null
        private var target: LivingEntity? = null
        private var damage: Double = 0.0
        private var cause: DamageCause? = null
        private val metadata: MutableMap<String, Any> = mutableMapOf()

        fun source(entity: LivingEntity?): Builder {
            this.source = entity
            return this
        }

        fun target(entity: LivingEntity): Builder {
            this.target = entity
            return this
        }

        fun damage(amount: Double): Builder {
            this.damage = amount
            return this
        }

        fun cause(cause: DamageCause): Builder {
            this.cause = cause
            return this
        }

        fun cause(name: String): Builder {
            this.cause = DamageCause.of(name)
            return this
        }

        fun metadata(key: String, value: Any): Builder {
            this.metadata[key] = value
            return this
        }

        fun build(): ProxyDamage {
            return ProxyDamage(
                source = source,
                target = requireNotNull(target) { "Target must be set" },
                baseDamage = damage,
                cause = cause ?: DamageCause.of("SKILL"),
                metadata = metadata.toMap()
            )
        }
    }
}
