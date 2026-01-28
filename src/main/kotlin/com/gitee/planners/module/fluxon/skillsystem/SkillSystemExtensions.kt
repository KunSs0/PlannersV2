package com.gitee.planners.module.fluxon.skillsystem

import com.gitee.planners.module.fluxon.FluxonScriptCache

import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 技能系统扩展
 */
object SkillSystemExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        // apAttack(params, targets) - 属性攻击
        runtime.registerFunction("apAttack", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val params = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getRef(1) as? List<*> ?: return@registerFunction
            val paramMap = parseAttackParams(params)
            val damage = paramMap["damage"]?.toDoubleOrNull() ?: 0.0
            targets.filterIsInstance<Entity>().forEach { entity ->
                if (entity is LivingEntity && !entity.isDead) {
                    entity.damage(damage)
                }
            }
            null
        }

        // apAttack(params, targets, source) - 属性攻击，带来源
        runtime.registerFunction("apAttack", returns(Type.VOID).params(Type.STRING, Type.OBJECT, Type.OBJECT)) { ctx ->
            val params = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getRef(1) as? List<*> ?: return@registerFunction
            val paramMap = parseAttackParams(params)
            val damage = paramMap["damage"]?.toDoubleOrNull() ?: 0.0
            targets.filterIsInstance<Entity>().forEach { entity ->
                if (entity is LivingEntity && !entity.isDead) {
                    entity.damage(damage)
                }
            }
            null
        }
    }

    private fun parseAttackParams(params: String): Map<String, String> {
        return params.split(",")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }
            .toMap()
    }
}
