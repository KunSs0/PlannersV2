package com.gitee.planners.core.skilltree

import com.gitee.planners.core.config.SkillTreeAttributeNode
import com.gitee.planners.core.config.SkillTreeNode
import com.gitee.planners.core.config.SkillTreeUnlockNode
import com.gitee.planners.core.config.SkillTreeSkillNode
import com.gitee.planners.core.player.PlayerTemplate

object SkillTreeNodeEffectService {

    fun getSkillLevel(template: PlayerTemplate, skillId: String): Int {
        val playerSkill = template.getRegisteredSkillOrNull(skillId)
        var level = if (playerSkill == null) {
            0
        } else {
            playerSkill.level
        }
        val contributions = collect(template)
        for (contribution in contributions) {
            if (contribution is SkillLevelNodeContribution && contribution.skillId == skillId) {
                level += contribution.level
            }
        }
        return level
    }

    fun getAttributes(template: PlayerTemplate): Map<String, Double> {
        val result = LinkedHashMap<String, Double>()
        val contributions = collect(template)
        for (contribution in contributions) {
            if (contribution !is AttributeNodeContribution) {
                continue
            }
            val current = result[contribution.attributeId]
            if (current == null) {
                result[contribution.attributeId] = contribution.amount
            } else {
                result[contribution.attributeId] = current + contribution.amount
            }
        }
        return result
    }

    /**
     * 收集职业继承链的属性与技能贡献，独立解锁记录由业务自行读取。
     *
     * @param template 玩家档案。
     * @return 已学习且拥有贡献 Provider 的节点效果。
     */
    private fun collect(template: PlayerTemplate): List<SkillTreeNodeContribution> {
        val router = template.playerRouter
        if (router == null) {
            return emptyList()
        }
        val result = ArrayList<SkillTreeNodeContribution>()
        for (route in router.routeLine) {
            for (tree in route.skillTrees) {
                for (node in tree.nodes.values) {
                    val state = route.getNodeStateOrNull(tree.id, node.id)
                    if (state == null || state.level <= 0) {
                        continue
                    }
                    // 解锁节点的效果由业务按等级读取，不产生属性或技能等级贡献。
                    if (node is SkillTreeUnlockNode) {
                        continue
                    }
                    val provider = findProvider(node)
                    if (provider == null) {
                        error("技能树节点 '${tree.id}:${node.id}' 找不到 Provider")
                    }
                    val context = SkillTreeNodeProviderContext(template, route, tree, node, state)
                    result.addAll(provider.provide(context))
                }
            }
        }
        return result
    }

    private fun findProvider(node: SkillTreeNode): SkillTreeNodeProvider? {
        if (node is SkillTreeSkillNode) {
            return SkillTreeNodeProviderRegistry.getOrNull("skill")
        }
        if (node is SkillTreeAttributeNode) {
            return SkillTreeNodeProviderRegistry.getOrNull(node.providerId)
        }
        return null
    }
}
