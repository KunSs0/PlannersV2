package com.gitee.planners.module.event.impl

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.bukkit.ActionBukkitEntityBuilder.getAnimated
import com.gitee.planners.module.event.ScriptBukkitEventHolder
import com.gitee.planners.module.event.animated.DamageEventModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import taboolib.module.kether.ScriptContext
import taboolib.platform.util.attacker
import taboolib.platform.util.getMetaFirst
import taboolib.platform.util.hasMeta

abstract class ScriptEntityEvent<T : EntityEvent> : ScriptBukkitEventHolder<T>() {

    override fun getSender(event: T): Target<*>? {
        return (event.entity as? Player)?.adaptTarget()
    }

    abstract class DamageEvent : ScriptEntityEvent<EntityDamageByEntityEvent>() {

        override val bind = EntityDamageByEntityEvent::class.java

        override fun handle(event: EntityDamageByEntityEvent, ctx: ScriptContext) {
            super.handle(event, ctx)
            ctx["damager"] = event.damager.adaptTarget()
            ctx["entity"] = event.entity.adaptTarget()
        }

        override fun getModifier(event: EntityDamageByEntityEvent): Animated? {
            return DamageEventModifier(event)
        }

    }

    object Damage : DamageEvent() {

        override val name = "damage"

        override fun getSender(event: EntityDamageByEntityEvent): Target<*>? {
            return (event.attacker as? Player)?.adaptTarget()
        }

    }

    object Damaged : DamageEvent() {

        override val name = "damaged"

    }


    object Death : ScriptEntityEvent<PlayerDeathEvent>() {

        override val name = "death"

        override val bind = PlayerDeathEvent::class.java

        override fun handle(event: PlayerDeathEvent, ctx: ScriptContext) {
            super.handle(event, ctx)
            // 识别 killer
            val killer = if (event.entity.hasMeta("@killer")) {
                event.entity.getMetaFirst("@killer").value() as LivingEntity
            } else {
                (event.entity.lastDamageCause as? EntityDamageByEntityEvent)?.attacker
            }
            ctx["attacker"] = killer
            ctx["message"] = event.deathMessage
        }


    }

    object ProjectileHit : ScriptEntityEvent<ProjectileHitEvent>() {

        override val name = "projectile.hit"

        override val bind = ProjectileHitEvent::class.java

        override fun getSender(event: ProjectileHitEvent): Target<*>? {
            if (event.entity.getAnimated() != null) return null

            return (event.entity.shooter as? Player)?.adaptTarget()
        }

        override fun handle(event: ProjectileHitEvent, ctx: ScriptContext) {
            ctx["entity"] = event.entity
            ctx["target"] = event.hitEntity?.adaptTarget()
            ctx["block"] = event.hitBlock?.location?.adaptTarget()
        }

    }

}
