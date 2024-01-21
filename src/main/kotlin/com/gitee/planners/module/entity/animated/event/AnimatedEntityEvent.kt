package com.gitee.planners.module.entity.animated.event

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedEvent
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.entity.animated.AbstractBukkitEntityAnimated
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import taboolib.module.kether.ScriptContext

abstract class AnimatedEntityEvent(val animated: AbstractBukkitEntityAnimated<*>, val entity: Entity) : AnimatedEvent {

    override fun inject(ctx: ScriptContext) {
        ctx["entity"] = entity
    }

    class Spawn(animated: AbstractBukkitEntityAnimated<*>,entity: Entity,val origin: Target<*>): AnimatedEntityEvent(animated,entity) {
        override val name = "spawn"

        override fun inject(ctx: ScriptContext) {
            super.inject(ctx)
            ctx["origin"] = origin
        }

    }

    class Hit(animated: AbstractBukkitEntityAnimated<*>, entity: Entity,val target: Entity?,val block: Block?) : AnimatedEntityEvent(animated, entity) {

        override val name = "hit"

        override fun inject(ctx: ScriptContext) {
            super.inject(ctx)
            ctx["target"] = target?.adaptTarget()
            ctx["block"] = block?.location?.adaptTarget()
        }

    }

}
