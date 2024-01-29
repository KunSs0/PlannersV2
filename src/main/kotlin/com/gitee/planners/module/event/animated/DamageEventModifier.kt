package com.gitee.planners.module.event.animated

import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import taboolib.common.platform.function.warning

class DamageEventModifier(original: EntityDamageByEntityEvent) : AbstractCancellableEvent<EntityDamageByEntityEvent>(original) {

    val source = text("source",getSource(original).name) {

    }

    val damage = double("damage",original.damage) {
        this@DamageEventModifier.original.damage = it
        // 直接修改最终值 避开 on update
        this@DamageEventModifier.finalDamage.any = this@DamageEventModifier.original.finalDamage
    }

    val finalDamage = double("final-damage",original.finalDamage) {
        warning("The final damage cannot be modified")
    }

    val cause = text("cause",original.cause.name) {
        warning("The cause cannot be modified")
    }

    companion object {

        fun getSource(event: EntityDamageByEntityEvent): Source {
            return if (event.damager is Projectile) {
                Source.PROJECTILE
            } else {
                Source.ENTITY
            }
        }

    }

    enum class Source {

        ENTITY, PROJECTILE;


    }

}
