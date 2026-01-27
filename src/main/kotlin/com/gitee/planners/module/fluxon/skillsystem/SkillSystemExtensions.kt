package com.gitee.planners.module.fluxon.skillsystem

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.fluxon.FluxonFunctionContext
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 技能系统扩展
 */
object SkillSystemExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getSkillLevel(skillId, [player]) -> int
        runtime.registerFunction("getSkillLevel", listOf(1, 2)) { ctx ->
            val skillId = ctx.arguments[0]?.toString() ?: return@registerFunction 0
            val player = ctx.getPlayerArg(1)
            player.plannersTemplate.getRegisteredSkillOrNull(skillId)?.level ?: 0
        }

        // setSkillLevel(skillId, level, [player]) -> void
        runtime.registerFunction("setSkillLevel", listOf(2, 3)) { ctx ->
            val skillId = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val level = (ctx.arguments[1] as Number).toInt()
            val player = ctx.getPlayerArg(2)
            val template = player.plannersTemplate
            val playerSkill = template.getRegisteredSkillOrNull(skillId) ?: return@registerFunction null
            PlayerTemplateAPI.setSkillLevel(template, playerSkill, level)
            null
        }

        // apAttack(params, targets, [player]) -> void
        runtime.registerFunction("apAttack", listOf(2, 3)) { ctx ->
            val params = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val targets = ctx.arguments[1] as? List<*> ?: return@registerFunction null
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

    private fun FluxonFunctionContext.getPlayerArg(index: Int): Player {
        if (arguments.size > index) {
            return arguments[index] as? Player
                ?: throw IllegalStateException("Argument at $index is not a player")
        }
        return environment.rootVariables["player"] as? Player
            ?: throw IllegalStateException("No player found in environment")
    }
}
