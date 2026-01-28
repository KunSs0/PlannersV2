package com.gitee.planners.module.fluxon.skillsystem

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getPlayerArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 技能系统扩展
 */
object SkillSystemExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        runtime.registerFunction("getSkillLevel", listOf(1, 2)) { ctx ->
            val skillId = ctx.getAsString(0) ?: return@registerFunction 0
            val player = ctx.getPlayerArg(1) ?: return@registerFunction 0
            player.plannersTemplate.getRegisteredSkillOrNull(skillId)?.level ?: 0
        }

        runtime.registerFunction("setSkillLevel", listOf(2, 3)) { ctx ->
            val skillId = ctx.getAsString(0) ?: return@registerFunction null
            val level = ctx.getAsInt(1)
            val player = ctx.getPlayerArg(2) ?: return@registerFunction null
            val template = player.plannersTemplate
            val playerSkill = template.getRegisteredSkillOrNull(skillId) ?: return@registerFunction null
            PlayerTemplateAPI.setSkillLevel(template, playerSkill, level)
            null
        }

        runtime.registerFunction("apAttack", listOf(2, 3)) { ctx ->
            val params = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getRef(1) as? List<*> ?: return@registerFunction null
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
