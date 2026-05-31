package com.gitee.planners.core.attribute.source

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * 从 ImmutableJob / ImmutableSkill 的 hook.attributes 读取属性。
 * key 在 registry 中 → 逻辑管线；否则 → 物理直通。
 */
class HookAttributeSource : AttributeSource {

    override val id = "hook"
    override val priority = AttributeSource.PRIORITY_SKILL

    override fun getAttributes(entity: LivingEntity): Map<String, Double> {
        if (entity !is Player) {
            return emptyMap()
        }
        val template = entity.plannersTemplate
        val route = template.route
        if (route == null) {
            return emptyMap()
        }
        val result = mutableMapOf<String, Double>()

        // Job hook.attributes
        val job = route.getJob() as? ImmutableJob
        if (job != null) {
            val options = ScriptOptions.common(entity)
            for ((key, expr) in job.attributes) {
                val value = eval(expr, options)
                if (value != null) {
                    result[key] = value
                }
            }
        }

        // Skill hook.attributes（已学习技能）
        for (skill in route.getImmutableSkillValues()) {
            val options = PlannersAPI.newOptions(entity, skill)
            for ((key, expr) in skill.attributes) {
                val value = eval(expr, options)
                if (value != null) {
                    val current = result[key]
                    if (current == null) {
                        result[key] = value
                    } else {
                        result[key] = current + value
                    }
                }
            }
        }
        return result
    }

    private fun eval(expr: String, options: ScriptOptions): Double? {
        val parsed = expr.toDoubleOrNull()
        if (parsed != null) {
            return parsed
        }
        return try {
            val script = SingletonScript(expr)
            val result = script.eval(options)
            result?.toString()?.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
