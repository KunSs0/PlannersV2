package com.gitee.planners.module.fluxon.projectile

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.*
import org.bukkit.util.Vector
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.platform.util.setMeta

/**
 * 投射物扩展
 */
object ProjectileExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // projectile(type, [speed], [targets]) - 发射投射物
        runtime.registerFunction("projectile", listOf(1, 2, 3)) { ctx ->
            val typeName = ctx.getAsString(0) ?: return@registerFunction null
            val speed = if (ctx.arguments.size > 1) ctx.getAsDouble(1) else 1.0
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            val type = ProjectileType.values().find {
                it.name.equals(typeName, ignoreCase = true)
            } ?: return@registerFunction null

            val projectiles = mutableListOf<Projectile>()
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                val projectile = entity.launchProjectile(type.clazz)
                projectile.velocity = projectile.velocity.multiply(speed)
                projectile.setMeta("@shooter", entity)
                projectiles.add(projectile)
            }
            projectiles
        }

        // projectileAt(type, direction, [speed], [targets]) - 向指定方向发射投射物
        runtime.registerFunction("projectileAt", listOf(4, 5, 6)) { ctx ->
            val typeName = ctx.getAsString(0) ?: return@registerFunction null
            val x = ctx.getAsDouble(1)
            val y = ctx.getAsDouble(2)
            val z = ctx.getAsDouble(3)
            val speed = if (ctx.arguments.size > 4) ctx.getAsDouble(4) else 1.0
            val targets = ctx.getTargetsArg(5, LeastType.SENDER)

            val type = ProjectileType.values().find {
                it.name.equals(typeName, ignoreCase = true)
            } ?: return@registerFunction null

            val direction = Vector(x, y, z).normalize()

            val projectiles = mutableListOf<Projectile>()
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                val projectile = entity.launchProjectile(type.clazz, direction.clone().multiply(speed))
                projectile.setMeta("@shooter", entity)
                projectiles.add(projectile)
            }
            projectiles
        }

        // projectileToward(type, [speed], [sources], [destinations]) - 向目标发射投射物
        runtime.registerFunction("projectileToward", listOf(2, 3, 4)) { ctx ->
            val typeName = ctx.getAsString(0) ?: return@registerFunction null
            val speed = if (ctx.arguments.size > 1) ctx.getAsDouble(1) else 1.0
            val sources = ctx.getTargetsArg(2, LeastType.SENDER)
            val destinations = ctx.getTargetsArg(3, LeastType.EMPTY)

            val type = ProjectileType.values().find {
                it.name.equals(typeName, ignoreCase = true)
            } ?: return@registerFunction null

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
            projectiles
        }
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
