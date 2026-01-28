package com.gitee.planners.module.fluxon.entity

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.platform.util.setMeta

/**
 * 实体生成与控制扩展
 */
object EntityExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // entitySpawn(type, [duration], [locations]) - 生成实体
        runtime.registerFunction("entitySpawn", listOf(1, 2, 3)) { ctx ->
            val typeName = ctx.getAsString(0) ?: return@registerFunction null
            val duration = if (ctx.arguments.size > 1) ctx.getAsLong(1) else -1L
            val locations = ctx.getTargetsArg(2, LeastType.ORIGIN)

            val type = runCatching {
                EntityType.valueOf(typeName.uppercase())
            }.getOrNull() ?: return@registerFunction null

            val entities = mutableListOf<Entity>()

            locations.filterIsInstance<ProxyTarget.Location<*>>().forEach { loc ->
                val world = loc.getBukkitWorld() ?: return@forEach
                val entity = world.spawnEntity(loc.getBukkitLocation(), type)

                if (duration > 0) {
                    entity.setMeta("@duration", duration)
                    submit(delay = duration) {
                        if (entity.isValid) {
                            entity.remove()
                        }
                    }
                }

                entities.add(entity)
            }

            if (entities.size == 1) entities.first().asTarget() else entities.map { it.asTarget() }
        }

        // entityRemove([targets]) - 移除实体
        runtime.registerFunction("entityRemove", listOf(0, 1)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.EMPTY)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance
                if (entity.isValid) {
                    entity.remove()
                }
            }
            null
        }

        // entityTeleport(x, y, z, [targets]) - 传送实体
        runtime.registerFunction("entityTeleport", listOf(3, 4)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance
                val loc = entity.location.clone()
                loc.x = x
                loc.y = y
                loc.z = z
                entity.teleport(loc)
            }
            null
        }

        // entityTeleportTo([targets], [destinations]) - 传送实体到目标位置
        runtime.registerFunction("entityTeleportTo", listOf(1, 2)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            val destinations = ctx.getTargetsArg(1, LeastType.EMPTY)

            val dest = destinations.filterIsInstance<ProxyTarget.Location<*>>().firstOrNull()
                ?: return@registerFunction null

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.teleport(dest.getBukkitLocation())
            }
            null
        }

        // entitySetAI(enabled, [targets]) - 设置实体 AI
        runtime.registerFunction("entitySetAI", listOf(1, 2)) { ctx ->
            val enabled = ctx.getRef(0) as? Boolean ?: true
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                entity.setAI(enabled)
            }
            null
        }

        // entitySetGravity(enabled, [targets]) - 设置实体重力
        runtime.registerFunction("entitySetGravity", listOf(1, 2)) { ctx ->
            val enabled = ctx.getRef(0) as? Boolean ?: true
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.setGravity(enabled)
            }
            null
        }

        // entitySetInvulnerable(enabled, [targets]) - 设置实体无敌
        runtime.registerFunction("entitySetInvulnerable", listOf(1, 2)) { ctx ->
            val enabled = ctx.getRef(0) as? Boolean ?: true
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isInvulnerable = enabled
            }
            null
        }

        // entitySetGlowing(enabled, [targets]) - 设置实体发光
        runtime.registerFunction("entitySetGlowing", listOf(1, 2)) { ctx ->
            val enabled = ctx.getRef(0) as? Boolean ?: true
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isGlowing = enabled
            }
            null
        }

        // entitySetSilent(enabled, [targets]) - 设置实体静音
        runtime.registerFunction("entitySetSilent", listOf(1, 2)) { ctx ->
            val enabled = ctx.getRef(0) as? Boolean ?: true
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isSilent = enabled
            }
            null
        }
    }
}
