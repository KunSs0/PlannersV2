package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.commandBool
import com.gitee.planners.module.kether.context.Context
import com.gitee.planners.module.kether.enumOrNull
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer

object Sort : AbstractSelector("sort") {

    /**
     * 使用方法: @sort <type> [reverse true/false]
     * @see SortType
     */
    override fun select() = KetherHelper.combinedKetherParser { instance ->
        instance.group(enumOrNull<SortType>(), commandBool("reverse", false)).apply(instance) { type, reverse ->
            now {
                if (type == null) {
                    error("Type is missing!")
                }
                val container = getTargetContainer()
                val newContainer = container.filter { type.predicate(it) }
                        .sortedWith(compareBy { type.compare(getEnvironmentContext(), it) })
                        .let { if (reverse) it.reversed() else it }
                container.clear()
                container.addAll(newContainer)
            }
        }
    }


    enum class SortType {
        NAME {
            override fun predicate(target: Target<*>) = target is Target.Named

            override fun compare(ctx: Context, target: Target<*>) = (target as Target.Named).getName()
        },
        DISTANCE {
            override fun predicate(target: Target<*>) = target is TargetLocation

            override fun compare(ctx: Context, target: Target<*>): Comparable<*> {
                val loc = (target as TargetLocation).getBukkitLocation()
                val origin = (ctx.origin as? TargetLocation)?.getBukkitLocation() ?: error("origin is not a location")
                return loc.distanceSquared(origin)
            }
        },
        RANDOM {
            override fun compare(ctx: Context, target: Target<*>) = Math.random()
        };


        open fun predicate(target: Target<*>): Boolean {
            return true
        }

        abstract fun compare(ctx: Context, target: Target<*>): Comparable<*>
    }

}