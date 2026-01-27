package com.gitee.planners.module.fluxon.skill

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonFunctionContext
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.LivingEntity
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

        // damage(amount, [target]) -> void
        runtime.registerFunction("damage", listOf(1, 2)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toDouble()
            val entity = ctx.getLivingEntityArg(1)
            entity.damage(amount)
            null
        }

        // damageWithSource(amount, source, [target]) -> void
        runtime.registerFunction("damageWithSource", listOf(2, 3)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toDouble()
            val killer = resolveLivingEntity(ctx.arguments[1])
            val entity = ctx.getLivingEntityArg(2)
            if (killer != null && killer != entity && entity.health <= amount) {
                entity.setMeta("@killer", killer)
            }
            entity.damage(amount)
            null
        }

        // heal(amount, [target]) -> void
        runtime.registerFunction("heal", listOf(1, 2)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toDouble()
            val entity = ctx.getLivingEntityArg(1)
            @Suppress("DEPRECATION")
            entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
            null
        }
    }

    private fun resolveLivingEntity(arg: Any?): LivingEntity? {
        return when (arg) {
            is ProxyTarget.BukkitEntity -> arg.instance as? LivingEntity
            is LivingEntity -> arg
            else -> null
        }
    }

    private fun FluxonFunctionContext.getLivingEntityArg(index: Int): LivingEntity {
        if (arguments.size > index) {
            return resolveLivingEntity(arguments[index])
                ?: throw IllegalStateException("Argument at $index is not a living entity")
        }
        val find = environment.rootVariables["target"] ?: environment.rootVariables["player"]
        return resolveLivingEntity(find)
            ?: throw IllegalStateException("No living entity found in environment")
    }
}
