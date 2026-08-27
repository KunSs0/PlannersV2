package com.gitee.planners.core.attribute.source

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
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
        val playerRouter = template.playerRouter
        if (playerRouter == null) {
            return emptyMap()
        }
        val result = mutableMapOf<String, Double>()

        // Job hook.attributes
        val job = playerRouter.getCurrentJob()
        val options = ScriptOptions.common(entity)
        for ((key, script) in job.attributeScripts) {
            val value = eval(script, options)
            if (value != null) {
                result[key] = value
            }
        }

        // Skill hook.attributes（已学习技能）
        for (skill in playerRouter.effectiveSkills.values) {
            val skillLevel = SkillTreeNodeEffectService.getSkillLevel(template, skill.id)
            if (skillLevel <= 0) {
                continue
            }
            val skillOptions = PlannersAPI.newOptions(entity, skill)
            for ((key, script) in skill.immutable.attributeScripts) {
                val value = eval(script, skillOptions)
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
        val nodeAttributes = SkillTreeNodeEffectService.getAttributes(template)
        for ((key, value) in nodeAttributes) {
            val current = result[key]
            if (current == null) {
                result[key] = value
            } else {
                result[key] = current + value
            }
        }
        return result
    }

    private fun eval(script: SingletonScript, options: ScriptOptions): Double? {
        val result = script.eval(options)
        if (result == null) {
            return null
        }
        return result.toString().toDoubleOrNull()
    }
}
