package com.gitee.planners.module.fluxon.projectile

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.entity.*
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.platform.util.setMeta

/**
 * 投射物扩展
 */
object ProjectileExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // projectile(type) - 发射投射物
        runtime.registerFunction("projectile", returns(Type.OBJECT).params(Type.STRING)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            launchProjectile(typeName, 1.0, targets)
        }

        // projectile(type, speed) - 发射投射物，指定速度
        runtime.registerFunction("projectile", returns(Type.OBJECT).params(Type.STRING, Type.NUMBER)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val speed = ctx.getAsDouble(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            launchProjectile(typeName, speed, targets)
        }

        // projectile(type, speed, targets) - 从目标发射投射物
        runtime.registerFunction("projectile", returns(Type.OBJECT).params(Type.STRING, Type.NUMBER, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val speed = ctx.getAsDouble(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            launchProjectile(typeName, speed, targets)
        }

        // projectileAt(type, x, y, z, speed) - 向指定方向发射投射物
        runtime.registerFunction("projectileAt", returns(Type.OBJECT).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val x = ctx.getAsDouble(1)
            val y = ctx.getAsDouble(2)
            val z = ctx.getAsDouble(3)
            val speed = ctx.getAsDouble(4)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            launchProjectileAt(typeName, Vector(x, y, z), speed, targets)
        }

        // projectileAt(type, x, y, z, speed, targets) - 从目标向指定方向发射投射物
        runtime.registerFunction("projectileAt", returns(Type.OBJECT).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val x = ctx.getAsDouble(1)
            val y = ctx.getAsDouble(2)
            val z = ctx.getAsDouble(3)
            val speed = ctx.getAsDouble(4)
            val targets = ctx.getTargetsArg(5, LeastType.SENDER)
            launchProjectileAt(typeName, Vector(x, y, z), speed, targets)
        }

        // projectileToward(type, speed) - 向目标发射投射物
        runtime.registerFunction("projectileToward", returns(Type.OBJECT).params(Type.STRING, Type.NUMBER)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val speed = ctx.getAsDouble(1)
            val sources = ctx.getTargetsArg(-1, LeastType.SENDER)
            val destinations = ctx.getTargetsArg(-1, LeastType.EMPTY)
            launchProjectileToward(typeName, speed, sources, destinations)
        }

        // projectileToward(type, speed, sources) - 从来源向目标发射投射物
        runtime.registerFunction("projectileToward", returns(Type.OBJECT).params(Type.STRING, Type.NUMBER, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val speed = ctx.getAsDouble(1)
            val sources = ctx.getTargetsArg(2, LeastType.SENDER)
            val destinations = ctx.getTargetsArg(-1, LeastType.EMPTY)
            launchProjectileToward(typeName, speed, sources, destinations)
        }

        // projectileToward(type, speed, sources, destinations) - 从来源向目标发射投射物
        runtime.registerFunction("projectileToward", returns(Type.OBJECT).params(Type.STRING, Type.NUMBER, Type.OBJECT, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val speed = ctx.getAsDouble(1)
            val sources = ctx.getTargetsArg(2, LeastType.SENDER)
            val destinations = ctx.getTargetsArg(3, LeastType.EMPTY)
            launchProjectileToward(typeName, speed, sources, destinations)
        }
    }

    private fun launchProjectile(typeName: String, speed: Double, targets: com.gitee.planners.api.job.target.ProxyTargetContainer): List<Projectile> {
        val type = ProjectileType.values().find { it.name.equals(typeName, ignoreCase = true) } ?: return emptyList()
        val projectiles = mutableListOf<Projectile>()

        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            val projectile = entity.launchProjectile(type.clazz)
            projectile.velocity = projectile.velocity.multiply(speed)
            projectile.setMeta("@shooter", entity)
            projectiles.add(projectile)
        }

        return projectiles
    }

    private fun launchProjectileAt(typeName: String, direction: Vector, speed: Double, targets: com.gitee.planners.api.job.target.ProxyTargetContainer): List<Projectile> {
        val type = ProjectileType.values().find { it.name.equals(typeName, ignoreCase = true) } ?: return emptyList()
        val normalizedDirection = direction.normalize()
        val projectiles = mutableListOf<Projectile>()

        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            val projectile = entity.launchProjectile(type.clazz, normalizedDirection.clone().multiply(speed))
            projectile.setMeta("@shooter", entity)
            projectiles.add(projectile)
        }

        return projectiles
    }

    private fun launchProjectileToward(typeName: String, speed: Double, sources: com.gitee.planners.api.job.target.ProxyTargetContainer, destinations: com.gitee.planners.api.job.target.ProxyTargetContainer): List<Projectile> {
        val type = ProjectileType.values().find { it.name.equals(typeName, ignoreCase = true) } ?: return emptyList()
        val projectiles = mutableListOf<Projectile>()

        sources.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { source ->
            val shooter = source.instance as? LivingEntity ?: return@forEach

            destinations.filterIsInstance<ProxyTarget.Location<*>>().forEach { dest ->
                val direction = dest.getBukkitLocation().toVector()
                    .subtract(shooter.eyeLocation.toVector())
                    .normalize()
                    .multiply(speed)

                val projectile = shooter.launchProjectile(type.clazz, direction)
                projectile.setMeta("@shooter", shooter)
                projectile.setMeta("@target", dest.getBukkitLocation())
                projectiles.add(projectile)
            }
        }

        return projectiles
    }

    enum class ProjectileType(val clazz: Class<out Projectile>) {
        ARROW(Arrow::class.java),
        DRAGON_FIREBALL(DragonFireball::class.java),
        EGG(Egg::class.java),
        ENDER_PEARL(EnderPearl::class.java),
        FIREBALL(Fireball::class.java),
        LARGE_FIREBALL(LargeFireball::class.java),
        SMALL_FIREBALL(SmallFireball::class.java),
        SNOWBALL(Snowball::class.java),
        SPECTRAL_ARROW(SpectralArrow::class.java),
        TRIDENT(Trident::class.java),
        WITHER_SKULL(WitherSkull::class.java),
        SHULKER_BULLET(ShulkerBullet::class.java),
        LLAMA_SPIT(LlamaSpit::class.java)
    }
}
