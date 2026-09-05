package com.gitee.planners.module.script

import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.api.damage.DamageCause
import com.gitee.planners.api.damage.ProxyDamage
import com.gitee.planners.api.effect.EffectProviders
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.compat.attribute.AttributeDriver
import com.gitee.planners.module.script.finder.TargetFinder
import com.novalang.bukkit.NovaBukkit
import org.bukkit.Sound
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

/** Planners 对外提供的 Nova JavaTypes 共享模块。 */
object PlannersCoreModule {

    const val MODULE_ID = "planners.core"

    private val types: List<Class<*>> = listOf(
        EntityFacingProviders::class.java,
        DamageCause::class.java,
        ProxyDamage::class.java,
        EffectProviders::class.java,
        Cooler::class.java,
        AttributeDriver::class.java,
        TargetFinder::class.java,
        Sound::class.java,
        PotionEffect::class.java,
        PotionEffectType::class.java,
        Vector::class.java,
    )

    /** 注册到 NovaLang 进程级共享模块表。 */
    fun register() {
        NovaBukkit.registerModule(MODULE_ID, types)
    }

    /** 从 NovaLang 进程级共享模块表注销。 */
    fun unregister() {
        NovaBukkit.unregisterModule(MODULE_ID)
    }
}
