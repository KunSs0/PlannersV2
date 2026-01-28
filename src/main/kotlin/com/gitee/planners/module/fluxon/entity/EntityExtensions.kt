package com.gitee.planners.module.fluxon.entity

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
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

        // entitySpawn(type) - 在 origin 生成实体
        runtime.registerFunction("entitySpawn", returns(Type.OBJECT).params(Type.STRING)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val locations = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            spawnEntities(typeName, -1L, locations)
        }

        // entitySpawn(type, duration) - 在 origin 生成实体，指定存活时长
        runtime.registerFunction("entitySpawn", returns(Type.OBJECT).params(Type.STRING, Type.J)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val locations = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            spawnEntities(typeName, duration, locations)
        }

        // entitySpawn(type, duration, locations) - 在指定位置生成实体
        runtime.registerFunction("entitySpawn", returns(Type.OBJECT).params(Type.STRING, Type.J, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val locations = ctx.getTargetsArg(2, LeastType.ORIGIN)
            spawnEntities(typeName, duration, locations)
        }

        // entityRemove() - 移除 targets 中的实体
        runtime.registerFunction("entityRemove", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.EMPTY)
            removeEntities(targets)
            null
        }

        // entityRemove(targets) - 移除指定实体
        runtime.registerFunction("entityRemove", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.EMPTY)
            removeEntities(targets)
            null
        }

        // entityTeleport(x, y, z) - 传送 sender
        runtime.registerFunction("entityTeleport", returns(Type.VOID).params(Type.D, Type.D, Type.D)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            teleportEntities(x, y, z, targets)
            null
        }

        // entityTeleport(x, y, z, targets) - 传送指定实体
        runtime.registerFunction("entityTeleport", returns(Type.VOID).params(Type.D, Type.D, Type.D, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            teleportEntities(x, y, z, targets)
            null
        }

        // entityTeleportTo(destinations) - 传送 sender 到目标位置
        runtime.registerFunction("entityTeleportTo", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            val destinations = ctx.getTargetsArg(0, LeastType.EMPTY)
            teleportTo(targets, destinations)
            null
        }

        // entityTeleportTo(targets, destinations) - 传送实体到目标位置
        runtime.registerFunction("entityTeleportTo", returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            val destinations = ctx.getTargetsArg(1, LeastType.EMPTY)
            teleportTo(targets, destinations)
            null
        }

        // entitySetAI(enabled) - 设置 sender AI
        runtime.registerFunction("entitySetAI", returns(Type.VOID).params(Type.BOOLEAN)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            setEntityAI(enabled, targets)
            null
        }

        // entitySetAI(enabled, targets) - 设置目标 AI
        runtime.registerFunction("entitySetAI", returns(Type.VOID).params(Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            setEntityAI(enabled, targets)
            null
        }

        // entitySetGravity(enabled) - 设置 sender 重力
        runtime.registerFunction("entitySetGravity", returns(Type.VOID).params(Type.BOOLEAN)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.setGravity(enabled)
            }
            null
        }

        // entitySetGravity(enabled, targets) - 设置目标重力
        runtime.registerFunction("entitySetGravity", returns(Type.VOID).params(Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.setGravity(enabled)
            }
            null
        }

        // entitySetInvulnerable(enabled) - 设置 sender 无敌
        runtime.registerFunction("entitySetInvulnerable", returns(Type.VOID).params(Type.BOOLEAN)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isInvulnerable = enabled
            }
            null
        }

        // entitySetInvulnerable(enabled, targets) - 设置目标无敌
        runtime.registerFunction("entitySetInvulnerable", returns(Type.VOID).params(Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isInvulnerable = enabled
            }
            null
        }

        // entitySetGlowing(enabled) - 设置 sender 发光
        runtime.registerFunction("entitySetGlowing", returns(Type.VOID).params(Type.BOOLEAN)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isGlowing = enabled
            }
            null
        }

        // entitySetGlowing(enabled, targets) - 设置目标发光
        runtime.registerFunction("entitySetGlowing", returns(Type.VOID).params(Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isGlowing = enabled
            }
            null
        }

        // entitySetSilent(enabled) - 设置 sender 静音
        runtime.registerFunction("entitySetSilent", returns(Type.VOID).params(Type.BOOLEAN)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isSilent = enabled
            }
            null
        }

        // entitySetSilent(enabled, targets) - 设置目标静音
        runtime.registerFunction("entitySetSilent", returns(Type.VOID).params(Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val enabled = ctx.getBool(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.isSilent = enabled
            }
            null
        }
    }

    /**
     * 在指定位置生成实体
     * @param typeName 实体类型名称（如 ZOMBIE, SKELETON）
     * @param duration 存活时长（tick，-1 表示永久）
     * @param locations 生成位置容器
     * @return 生成的实体或实体列表（单个时返回 ProxyTarget，多个时返回 List）
     */
    private fun spawnEntities(typeName: String, duration: Long, locations: com.gitee.planners.api.job.target.ProxyTargetContainer): Any? {
        val type = runCatching { EntityType.valueOf(typeName.uppercase()) }.getOrNull() ?: return null
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

        return if (entities.size == 1) entities.first().asTarget() else entities.map { it.asTarget() }
    }

    /**
     * 移除目标容器中的所有实体
     * @param targets 目标容器
     */
    private fun removeEntities(targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance
            if (entity.isValid) {
                entity.remove()
            }
        }
    }

    /**
     * 传送实体到指定坐标（保持世界和朝向不变）
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param targets 目标容器
     */
    private fun teleportEntities(x: Double, y: Double, z: Double, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance
            val loc = entity.location.clone()
            loc.x = x
            loc.y = y
            loc.z = z
            entity.teleport(loc)
        }
    }

    /**
     * 传送实体到目标位置
     * @param targets 要传送的实体容器
     * @param destinations 目标位置容器（使用第一个位置）
     */
    private fun teleportTo(targets: com.gitee.planners.api.job.target.ProxyTargetContainer, destinations: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val dest = destinations.filterIsInstance<ProxyTarget.Location<*>>().firstOrNull() ?: return
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            target.instance.teleport(dest.getBukkitLocation())
        }
    }

    /**
     * 设置实体的 AI 状态
     * @param enabled 是否启用 AI
     * @param targets 目标容器（仅 LivingEntity 生效）
     */
    private fun setEntityAI(enabled: Boolean, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            entity.setAI(enabled)
        }
    }
}
