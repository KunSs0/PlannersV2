package com.gitee.planners.module.fluxon.skillsystem

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * 技能系统扩展
 */
object SkillSystemExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player 技能等级管理扩展
        runtime.registerExtension(Player::class.java)
            .function("getSkillLevel", FunctionSignature.returns(Type.I).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val skillId = ctx.getRef(0)?.toString() ?: return@function

                val template = player.plannersTemplate
                val playerSkill = template.getRegisteredSkillOrNull(skillId)
                ctx.setReturnInt(playerSkill?.level ?: 0)
            }
            .function("setSkillLevel", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.I)) { ctx ->
                val player = ctx.target ?: return@function
                val skillId = ctx.getRef(0)?.toString() ?: return@function
                val level = ctx.getAsInt(1)

                val template = player.plannersTemplate
                val playerSkill = template.getRegisteredSkillOrNull(skillId) ?: return@function
                PlayerTemplateAPI.setSkillLevel(template, playerSkill, level)
            }

        // 属性攻击（通过 Player 扩展）
        runtime.registerExtension(Player::class.java)
            .function("apAttack", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
                ctx.target ?: return@function
                val params = ctx.getRef(0)?.toString() ?: return@function
                val targets = ctx.getRef(1) as? List<*> ?: return@function

                // 解析参数（格式示例: "damage=100,type=physical"）
                val paramMap = parseAttackParams(params)
                val damage = paramMap["damage"]?.toDoubleOrNull() ?: 0.0

                // 对所有目标造成伤害
                targets.filterIsInstance<Entity>().forEach { entity ->
                    if (entity is LivingEntity && !entity.isDead) {
                        entity.damage(damage)
                    }
                }
            }
    }

    /**
     * 解析攻击参数
     * @param params 参数字符串，格式: "key1=value1,key2=value2"
     * @return 参数映射
     */
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
}
