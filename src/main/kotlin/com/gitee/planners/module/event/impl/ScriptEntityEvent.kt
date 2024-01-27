package com.gitee.planners.module.event.impl

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.action.bukkit.ActionBukkitEntity.getAnimated
import com.gitee.planners.module.event.ScriptBukkitEventWrapped
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.module.kether.ScriptContext
import taboolib.platform.util.attacker

abstract class ScriptEntityEvent<T : EntityEvent> : ScriptBukkitEventWrapped<T> {

    override fun getSender(event: T): Target<*>? {
        return (event.entity as? Player)?.adaptTarget()
    }

    override fun handle(event: T, ctx: ScriptContext) {

    }


    abstract class DamageEvent : ScriptEntityEvent<EntityDamageByEntityEvent>() {

        override val bind = EntityDamageByEntityEvent::class.java

        override fun handle(event: EntityDamageByEntityEvent, ctx: ScriptContext) {
            super.handle(event, ctx)
            ctx["damager"] = event.damager.adaptTarget()
            ctx["entity"] = event.entity.adaptTarget()
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
            val el = event.entity.lastDamageCause as? EntityDamageByEntityEvent ?: return
            ctx["attacker"] = el.attacker
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
