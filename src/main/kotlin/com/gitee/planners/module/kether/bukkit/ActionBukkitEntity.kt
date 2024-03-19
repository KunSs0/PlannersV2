package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.Planners
import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedListener
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.common.util.DefaultNearestEntityFinder
import com.gitee.planners.api.common.util.NearestEntityFinder
import com.gitee.planners.api.common.util.PathTrace
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext
import com.gitee.planners.module.kether.context.Context
import com.gitee.planners.api.job.target.*
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.module.kether.*
import com.gitee.planners.module.entity.animated.AbstractBukkitEntityAnimated
import com.gitee.planners.module.entity.animated.BukkitEntity
import com.gitee.planners.module.entity.animated.event.AnimatedEntityEvent
import com.gitee.planners.util.syncing
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common5.Baffle
import taboolib.module.kether.ScriptFrame
import taboolib.platform.util.getMetaFirstOrNull
import taboolib.platform.util.setMeta
import java.util.concurrent.TimeUnit

@CombinationKetherParser.Used
object ActionBukkitEntity : MultipleKetherParser("entity") {

    @KetherEditor.Document("entity create <entity-type> <validity/tick>")
    val create = KetherHelper.combinedKetherParser {
        it.group(enum<EntityType>(), int()).apply(it) { type,tick ->
            now {
                BukkitEntity(type,tick)
            }
        }
    }

    @KetherEditor.Document("entity spawn <animated> [at objective:TargetContainer(sender)]")
    val spawn = KetherHelper.combinedKetherParser {
        it.group(any(), commandObjectiveOrOrigin()).apply(it) { animated,objective ->
            animated as? AbstractBukkitEntityAnimated<*> ?: error("Animated object is not supported")
            now {
                val container = TargetContainer.of(*objective.map { this.createBukkitEntity(animated,it) }.toTypedArray())
                container
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    @KetherEditor.Document("entity launch <animated> <time/tick> [at objective:TargetContainer(sender)] <to objective:TargetContainer>")
    val launch = KetherHelper.combinedKetherParser {
        it.group(any(), long(), commandObjectiveOrEmpty(), commandObjectiveOrEmpty("to")).apply(it) { animated, time,origin, objective ->
            animated as? BukkitEntity ?: error("[BukkitEntity] Animated object is not supported")
            val origin = origin.filterIsInstance<TargetLocation<*>>().firstOrNull() ?: error("[BukkitEntity] origin cannot be empty")
            val objective = objective.filterIsInstance<TargetLocation<*>>().firstOrNull() ?: error("[BukkitEntity] objective cannot be empty")
            // Check if the distance is too short. FIXME: Maybe we should silent this when there is no target?
            if (origin.getX() == objective.getX() && origin.getY() == objective.getY() && origin.getZ() == objective.getZ()) {
                error("[BukkitEntity] The position of the origin and the target is the same! " +
                        "Did you excluded the origin (sender) in the target selection?")
            }
            val distance = objective.getBukkitLocation().distance(origin.getBukkitLocation())
            val accuracy = distance / time
            val direction = objective.getBukkitLocation().toVector().subtract(origin.getBukkitLocation().toVector()).normalize()
            val trace = PathTrace(origin.getBukkitLocation().toVector(), direction)
            now {
                val entity = createBukkitEntity(animated,origin).getInstance() as LivingEntity
                val context = getEnvironmentContext() as AbstractComplexScriptContext
                var tick = 0L
                val vectors = trace.traces(distance, accuracy).toList()
                val baffle = Baffle.of(1000, TimeUnit.MILLISECONDS)
                submit(period = 1) {
                    if (tick == time) {
                        cancel()
                        return@submit
                    }
                    val location = vectors[tick.toInt()].toLocation(entity.world)
                    // 如果被阻拦 并且有障碍物（非空气 非液体）
                    if (animated.isObstacle.asBoolean() && (location.block.type !in Planners.unimpededTypes.get())) {
                        cancel()
                        info("on obstacle ${location.block}")
                        entity.setMeta("clearable","obstacle")
                        return@submit
                    }
                    entity.teleport(location)
                    val nearestEntityFinder = DefaultNearestEntityFinder(location, syncing { location.world!!.entities }.get())
                    nearestEntityFinder.request().filter { it != entity && baffle.hasNext(it.uniqueId.toString(),true) }.forEach {
                        // 如果非自由节点 并且命中了 origin 直接过滤掉本次
                        if (!animated.isFreedom.asBoolean() && it == origin.getInstance()) {
                            return@forEach
                        }
                        animated.emit(AnimatedEntityEvent.Hit(animated,entity,it,null),context)
                    }
                    tick++
                }
                entity
            }

        }
    }

    @KetherEditor.Document("entity remove <entity>")
    val remove = KetherHelper.combinedKetherParser {
        it.group(any()).apply(it) {
            val entity = it as Entity
            now {
                val animated = entity.getMetaFirstOrNull("@animated")?.value() as? BukkitEntity ?: return@now
                animated.getClearableTask(entity)?.close()
            }
        }
    }


    @KetherEditor.Document("entity listen <animated> on <event> then <function>")
    val listen = KetherHelper.combinedKetherParser {
        it.group(any(), command("on", then = text()), command("then", then = any())).apply(it) { animated,event,funcId ->
            animated as? AbstractBukkitEntityAnimated<*> ?: error("Animated object is not supported")
            now {
                // on listen
                animated.listen(AnimatedListener(event,funcId.toString()))
            }
        }
    }

    private fun ScriptFrame.createBukkitEntity(animated: AbstractBukkitEntityAnimated<*>,target: Target<*>): TargetBukkitEntity {
        val context = this.getEnvironmentContext() as AbstractComplexScriptContext
        // 拉到主线程创建实体
        val entity = syncing { animated.invokeSpawn(target) }.get()
        entity.setMeta("@caster",target)
        entity.setMeta("@animated",animated)
        entity.setMeta("@context",context)
        animated.emit(AnimatedEntityEvent.Spawn(animated,entity,target), context)
        return entity.adaptTarget()
    }

    fun Entity.getAnimated(): Animated? {
        return this.getMetaFirstOrNull("@animated")?.value() as? Animated
    }

    fun Entity.getCasterTarget(): Target<*>? {
        return this.getMetaFirstOrNull("@caster")?.value() as? Target<*>
    }

    fun Entity.getCasterContext(): Context? {
        return this.getMetaFirstOrNull("@context")?.value() as? Context
    }



}
