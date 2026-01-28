package com.gitee.planners.module.fluxon.skillsystem

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 技能系统扩展
 */
object SkillSystemExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:skill", "skill", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(SkillObject)
        }
        runtime.exportRegistry.registerClass(SkillObject::class.java, "pl:skill")
    }

    object SkillObject {

        @JvmField
        val TYPE: Type = Type.fromClass(SkillObject::class.java)

        @Export
        fun getLevel(skillId: String, @Optional player: Player): Int {
            return player.plannersTemplate.getRegisteredSkillOrNull(skillId)?.level ?: 0
        }

        @Export
        fun setLevel(skillId: String, level: Int, @Optional player: Player) {
            val template = player.plannersTemplate
            val playerSkill = template.getRegisteredSkillOrNull(skillId) ?: return
            PlayerTemplateAPI.setSkillLevel(template, playerSkill, level)
        }

        @Export
        fun apAttack(params: String, targets: List<*>, @Optional player: Player) {
            val paramMap = parseAttackParams(params)
            val damage = paramMap["damage"]?.toDoubleOrNull() ?: 0.0
            targets.filterIsInstance<Entity>().forEach { entity ->
                if (entity is LivingEntity && !entity.isDead) {
                    entity.damage(damage)
                }
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
}
