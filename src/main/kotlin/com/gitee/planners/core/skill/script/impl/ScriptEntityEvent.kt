package com.gitee.planners.core.skill.script.impl

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.skill.script.ScriptBukkitEventHolder
import com.gitee.planners.core.skill.script.animated.DamageEventModifier
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import taboolib.platform.util.attacker
import taboolib.platform.util.getMetaFirst
import taboolib.platform.util.hasMeta

abstract class ScriptEntityEvent<T : EntityEvent> : ScriptBukkitEventHolder<T>() {

    override fun getSender(event: T): ProxyTarget<*>? {
        return (event.entity as? Player)?.asTarget()
    }

    abstract class DamageEvent : ScriptEntityEvent<EntityDamageByEntityEvent>() {

        override val bind = EntityDamageByEntityEvent::class.java

        override fun handle(event: EntityDamageByEntityEvent, options: FluxonScriptOptions) {
            super.handle(event, options)
            options.set("damager", event.damager.asTarget())
            options.set("entity", event.entity.asTarget())
        }

        override fun getModifier(event: EntityDamageByEntityEvent): Animated? {
            return DamageEventModifier(event)
        }

    }

    object Damage : DamageEvent() {

        override val name = "damage"

        override fun getSender(event: EntityDamageByEntityEvent): ProxyTarget<*>? {
            return (event.attacker as? Player)?.asTarget()
        }

    }

    object Damaged : DamageEvent() {

        override val name = "damaged"

    }


    object Death : ScriptEntityEvent<PlayerDeathEvent>() {

        override val name = "death"

        override val bind = PlayerDeathEvent::class.java

        override fun handle(event: PlayerDeathEvent, options: FluxonScriptOptions) {
            super.handle(event, options)
            // 识别 killer
            val killer = if (event.entity.hasMeta("@killer")) {
                event.entity.getMetaFirst("@killer").value() as LivingEntity
            } else {
                (event.entity.lastDamageCause as? EntityDamageByEntityEvent)?.attacker
            }
            options.set("attacker", killer)
            options.set("message", event.deathMessage)
        }


    }

    object ProjectileHit : ScriptEntityEvent<ProjectileHitEvent>() {

        override val name = "projectile.hit"

        override val bind = ProjectileHitEvent::class.java

        override fun getSender(event: ProjectileHitEvent): ProxyTarget<*>? {
            // 检查是否是动画实体（需要移除对旧kether的依赖）
            return (event.entity.shooter as? Player)?.asTarget()
        }

        override fun handle(event: ProjectileHitEvent, options: FluxonScriptOptions) {
            options.set("entity", event.entity)
            options.set("target", event.hitEntity?.asTarget())
            options.set("block", event.hitBlock?.location?.asTarget())
        }

    }

}
