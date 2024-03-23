package com.gitee.planners.module.entity.animated

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.util.rotateAroundX
import com.gitee.planners.util.rotateAroundY
import com.gitee.planners.util.rotateAroundZ
import org.bukkit.entity.*
import taboolib.common.util.Vector

class BukkitProjectileBuilder(val type: Type, step: Float) : AbstractBukkitEntityAnimated<Projectile>(), EntitySpawner {


    /** 自动删除策略 */
    val isAutoRemove = bool("is-auto-remove", true) {

    }

    val step = float("step", step) {

    }

    val rotation = vector("rotation", Vector(0.0, 0.0, 0.0)) {

    }

    // 向量
    val velocity = vector("velocity", Vector(0, 0, 0)) {
        val velocity = this@BukkitProjectileBuilder.instance.velocity
        val rotation = rotation.asVector()
        rotateAroundX(velocity, rotation.x)
        rotateAroundY(velocity, rotation.y)
        rotateAroundZ(velocity, rotation.z)
        velocity.multiply(this@BukkitProjectileBuilder.step.asFloat())
        this@BukkitProjectileBuilder.instance.velocity = velocity
    }

    val isBounce = bool("is-bounce", false) {
        this@BukkitProjectileBuilder.instance.setBounce(it)
    }

    override fun create(target: Target<*>): Projectile {
        val bukkitEntity = (target as TargetBukkitEntity).instance as LivingEntity
        return bukkitEntity.launchProjectile(this.type.clazz)
    }

    enum class Type(val clazz: Class<out Projectile>) {

        ARROW(Arrow::class.java),

        DRAGON_FIRE_BALL(DragonFireball::class.java),

        EGG(Egg::class.java),

        ENDER_PEARL(EnderPearl::class.java),

        FIRE_BALL(Fireball::class.java),

        FISH_HOOK(FishHook::class.java),

        LARGE_FIRE_BALL(LargeFireball::class.java),

        LINGERING_POTION(LingeringPotion::class.java),

        LLAMA_SPIT(LlamaSpit::class.java),

        SHULKER_BULLET(ShulkerBullet::class.java),

        SMALL_FIRE_BALL(SmallFireball::class.java),

        SNOW_BALL(Snowball::class.java),

        SPECTRAL_ARROW(SpectralArrow::class.java),

        SPLASH_POTION(SplashPotion::class.java),

        THROWN_EXP_BOTTLE(ThrownExpBottle::class.java),

        THROWN_POTION(ThrownPotion::class.java),

        TIPPED_ARROW(TippedArrow::class.java),

        WITHER_SKULL(WitherSkull::class.java)
    }


}
