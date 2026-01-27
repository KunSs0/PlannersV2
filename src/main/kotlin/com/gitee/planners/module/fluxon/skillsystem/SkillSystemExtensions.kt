package com.gitee.planners.module.fluxon.skillsystem

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionContext
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 技能系统扩展
 */
object SkillSystemExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getSkillLevel(skillId) -> int (从环境获取player)
        runtime.registerFunction("getSkillLevel", FunctionSignature.returns(Type.I).params(Type.OBJECT)) { ctx ->
            val skillId = ctx.getRef(0)?.toString() ?: return@registerFunction
            val player = getPlayerFromEnv(ctx)
            val template = player.plannersTemplate
            val playerSkill = template.getRegisteredSkillOrNull(skillId)
            ctx.setReturnInt(playerSkill?.level ?: 0)
        }

        // getSkillLevel(skillId, player) -> int
        runtime.registerFunction("getSkillLevel", FunctionSignature.returns(Type.I).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skillId = ctx.getRef(0)?.toString() ?: return@registerFunction
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            val template = player.plannersTemplate
            val playerSkill = template.getRegisteredSkillOrNull(skillId)
            ctx.setReturnInt(playerSkill?.level ?: 0)
        }

        // setSkillLevel(skillId, level) -> void (从环境获取player)
        runtime.registerFunction("setSkillLevel", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.I)) { ctx ->
            val skillId = ctx.getRef(0)?.toString() ?: return@registerFunction
            val level = ctx.getAsInt(1)
            val player = getPlayerFromEnv(ctx)
            val template = player.plannersTemplate
            val playerSkill = template.getRegisteredSkillOrNull(skillId) ?: return@registerFunction
            PlayerTemplateAPI.setSkillLevel(template, playerSkill, level)
        }

        // setSkillLevel(skillId, level, player) -> void
        runtime.registerFunction("setSkillLevel", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.I, Type.OBJECT)) { ctx ->
            val skillId = ctx.getRef(0)?.toString() ?: return@registerFunction
            val level = ctx.getAsInt(1)
            val player = ctx.getRef(2) as? Player ?: return@registerFunction
            val template = player.plannersTemplate
            val playerSkill = template.getRegisteredSkillOrNull(skillId) ?: return@registerFunction
            PlayerTemplateAPI.setSkillLevel(template, playerSkill, level)
        }

        // apAttack(params, targets) -> void (从环境获取player)
        runtime.registerFunction("apAttack", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val params = ctx.getRef(0)?.toString() ?: return@registerFunction
            val targets = ctx.getRef(1) as? List<*> ?: return@registerFunction
            val paramMap = parseAttackParams(params)
            val damage = paramMap["damage"]?.toDoubleOrNull() ?: 0.0
            targets.filterIsInstance<Entity>().forEach { entity ->
                if (entity is LivingEntity && !entity.isDead) {
                    entity.damage(damage)
                }
            }
        }

        // apAttack(params, targets, player) -> void
        runtime.registerFunction("apAttack", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT, Type.OBJECT)) { ctx ->
            val params = ctx.getRef(0)?.toString() ?: return@registerFunction
            val targets = ctx.getRef(1) as? List<*> ?: return@registerFunction
            val paramMap = parseAttackParams(params)
            val damage = paramMap["damage"]?.toDoubleOrNull() ?: 0.0
            targets.filterIsInstance<Entity>().forEach { entity ->
                if (entity is LivingEntity && !entity.isDead) {
                    entity.damage(damage)
                }
            }
        }
    }

    private fun parseAttackParams(params: String): Map<String, String> {
        return params.split(",")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else null
            }
            .toMap()
    }

    private fun getPlayerFromEnv(ctx: FunctionContext<*>): Player {
        val find = ctx.environment.rootVariables["player"]
        if (find is Player) {
            return find
        }
        throw IllegalStateException("No player found in environment")
    }
}
