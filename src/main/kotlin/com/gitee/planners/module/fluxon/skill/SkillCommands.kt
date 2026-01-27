package com.gitee.planners.module.fluxon.skill

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionContext
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
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

        // damage(amount) -> void (从环境获取target)
        runtime.registerFunction("damage", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val entity = getLivingEntityFromEnv(ctx)
            entity.damage(amount)
        }

        // damage(amount, target) -> void
        runtime.registerFunction("damage", FunctionSignature.returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targetArg = ctx.getRef(1)
            val entity = resolveLivingEntity(targetArg) ?: return@registerFunction
            entity.damage(amount)
        }

        // damageWithSource(amount, source) -> void (从环境获取target)
        runtime.registerFunction("damageWithSource", FunctionSignature.returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val sourceArg = ctx.getRef(1)
            val entity = getLivingEntityFromEnv(ctx)
            val killer = resolveLivingEntity(sourceArg)
            if (killer != null && killer != entity) {
                if (entity.health <= amount) {
                    entity.setMeta("@killer", killer)
                }
            }
            entity.damage(amount)
        }

        // damageWithSource(amount, source, target) -> void
        runtime.registerFunction("damageWithSource", FunctionSignature.returns(Type.VOID).params(Type.D, Type.OBJECT, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val sourceArg = ctx.getRef(1)
            val targetArg = ctx.getRef(2)
            val entity = resolveLivingEntity(targetArg) ?: return@registerFunction
            val killer = resolveLivingEntity(sourceArg)
            if (killer != null && killer != entity) {
                if (entity.health <= amount) {
                    entity.setMeta("@killer", killer)
                }
            }
            entity.damage(amount)
        }

        // heal(amount) -> void (从环境获取target)
        runtime.registerFunction("heal", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val entity = getLivingEntityFromEnv(ctx)
            entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
        }

        // heal(amount, target) -> void
        runtime.registerFunction("heal", FunctionSignature.returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targetArg = ctx.getRef(1)
            val entity = resolveLivingEntity(targetArg) ?: return@registerFunction
            entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
        }
    }

    private fun resolveLivingEntity(arg: Any?): LivingEntity? {
        return when (arg) {
            is ProxyTarget.BukkitEntity -> arg.instance as? LivingEntity
            is LivingEntity -> arg
            else -> null
        }
    }

    private fun getLivingEntityFromEnv(ctx: FunctionContext<*>): LivingEntity {
        val find = ctx.environment.rootVariables["target"]
            ?: ctx.environment.rootVariables["player"]
        return resolveLivingEntity(find)
            ?: throw IllegalStateException("No living entity found in environment")
    }
}
