package com.gitee.planners.module.fluxon.skill

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.platform.util.setMeta

/**
 * 技能相关扩展函数注册
 */
object SkillCommands {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:combat", "combat", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(CombatObject)
        }
        runtime.exportRegistry.registerClass(CombatObject::class.java, "pl:combat")
    }

    object CombatObject {

        @JvmField
        val TYPE: Type = Type.fromClass(CombatObject::class.java)

        @Export
        fun damage(amount: Double, @Optional target: LivingEntity) {
            target.damage(amount)
        }

        @Export
        fun damageWithSource(amount: Double, source: Any?, @Optional target: LivingEntity) {
            val killer = resolveLivingEntity(source)
            if (killer != null && killer != target && target.health <= amount) {
                target.setMeta("@killer", killer)
            }
            target.damage(amount)
        }

        @Export
        fun heal(amount: Double, @Optional target: LivingEntity) {
            @Suppress("DEPRECATION")
            target.health = (target.health + amount).coerceAtMost(target.maxHealth)
        }

        private fun resolveLivingEntity(arg: Any?): LivingEntity? {
            return when (arg) {
                is ProxyTarget.BukkitEntity -> arg.instance as? LivingEntity
                is LivingEntity -> arg
                else -> null
            }
        }
    }
}
