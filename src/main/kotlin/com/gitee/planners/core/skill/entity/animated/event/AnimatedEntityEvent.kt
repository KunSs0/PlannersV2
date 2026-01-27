package com.gitee.planners.core.skill.entity.animated.event

import com.gitee.planners.api.common.entity.animated.AnimatedEvent
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.skill.entity.animated.AbstractBukkitEntityAnimated
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import org.bukkit.block.Block
import org.bukkit.entity.Entity

abstract class AnimatedEntityEvent(val animated: AbstractBukkitEntityAnimated<*>, val entity: Entity) : AnimatedEvent {

    override fun inject(options: FluxonScriptOptions) {
        options.set("entity", entity)
    }

    class Spawn(animated: AbstractBukkitEntityAnimated<*>, entity: Entity, val origin: Target<*>): AnimatedEntityEvent(animated,entity) {
        override val name = "spawn"

        override fun inject(options: FluxonScriptOptions) {
            super.inject(options)
            options.set("origin", origin)
        }

    }

    class Hit(animated: AbstractBukkitEntityAnimated<*>, entity: Entity, val target: Entity?, val block: Block?) : AnimatedEntityEvent(animated, entity) {

        override val name = "hit"

        override fun inject(options: FluxonScriptOptions) {
            super.inject(options)
            options.set("target", target?.asTarget())
            options.set("block", block?.location?.asTarget())
        }

    }

}
