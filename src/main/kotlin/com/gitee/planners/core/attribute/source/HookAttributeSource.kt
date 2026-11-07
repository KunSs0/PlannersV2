package com.gitee.planners.core.attribute.source

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import com.gitee.planners.module.action.ActionScopeFactory
import com.gitee.planners.module.action.AttributeExpressions
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * 从 ImmutableJob / ImmutableSkill 的 hook.attributes 读取属性。
 * key 在 registry 中 → 逻辑管线；否则 → 物理直通。
 *
 * 表达式统一由查询引擎求值：职业属性用 `profile.*`，技能属性用 `skill.*`。
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

        applyJobAttributes(entity, playerRouter.getCurrentJob().attributeExpressions, result)
        applySkillAttributes(entity, playerRouter, result)

        val nodeAttributes = SkillTreeNodeEffectService.getAttributes(template)
        for ((key, value) in nodeAttributes) {
            accumulate(result, key, value)
        }
        return result
    }

    /**
     * 累加职业属性。
     *
     * @param player 玩家。
     * @param expressions 已编译的属性表达式。
     * @param result 属性累加结果。
     */
    private fun applyJobAttributes(
        player: Player,
        expressions: Map<String, com.gitee.planners.module.action.query.QueryRef>,
        result: MutableMap<String, Double>,
    ) {
        if (expressions.isEmpty()) {
            return
        }
        val scope = ActionScopeFactory.forPlayer(player, null, 0, emptyMap(), null, "job-attribute")
        for (entry in expressions) {
            val value = AttributeExpressions.evaluate(entry.value, scope)
            if (value != null) {
                accumulate(result, entry.key, value)
            }
        }
    }

    /**
     * 累加已学习技能的属性。
     *
     * @param player 玩家。
     * @param playerRouter 当前职业线。
     * @param result 属性累加结果。
     */
    private fun applySkillAttributes(
        player: Player,
        playerRouter: com.gitee.planners.core.player.PlayerRouter,
        result: MutableMap<String, Double>,
    ) {
        val template = player.plannersTemplate
        for (skill in playerRouter.effectiveSkills.values) {
            val expressions = skill.immutable.attributeExpressions
            if (expressions.isEmpty()) {
                continue
            }
            val skillLevel = SkillTreeNodeEffectService.getSkillLevel(template, skill.id)
            if (skillLevel <= 0) {
                continue
            }
            val scope = createSkillScope(player, skill.immutable, skillLevel)
            for (entry in expressions) {
                val value = AttributeExpressions.evaluate(entry.value, scope)
                if (value != null) {
                    accumulate(result, entry.key, value)
                }
            }
        }
    }

    /**
     * 构造技能属性的求值上下文。
     *
     * @param player 玩家。
     * @param skill 技能定义。
     * @param skillLevel 技能等级。
     * @return 查询上下文。
     */
    private fun createSkillScope(player: Player, skill: ImmutableSkill, skillLevel: Int): QueryScope {
        return ActionScopeFactory.forPlayer(
            player,
            skill,
            skillLevel,
            emptyMap(),
            null,
            "skill:" + skill.id + ":attribute",
        )
    }

    /**
     * 累加单个属性；同一属性多次出现时求和。
     *
     * @param result 属性累加结果。
     * @param key 属性键。
     * @param value 本次数值。
     */
    private fun accumulate(result: MutableMap<String, Double>, key: String, value: Double) {
        val current = result[key]
        if (current == null) {
            result[key] = value
            return
        }
        result[key] = current + value
    }
}
