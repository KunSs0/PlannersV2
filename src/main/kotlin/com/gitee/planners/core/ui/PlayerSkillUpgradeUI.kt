package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeSkillNode
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.formatter.DynamicSkillIcon
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import org.bukkit.entity.Player
import taboolib.platform.util.sendLang

object PlayerSkillUpgradeUI : AutomationBaseUI("skill-upgrade.yml") {

    override fun display(player: Player): BaseUI.Display {
        throw UnsupportedOperationException("Use open(player, skill) instead")
    }

    fun open(player: Player, skill: ImmutableSkill) {
        val router = player.plannersTemplate.playerRouter
        if (router == null) {
            return
        }
        val route = router.getRouteForSkill(skill.id)
        if (route == null) {
            return
        }
        val target = findSkillNode(route, skill.id)
        if (target == null) {
            player.sendLang("skill-upgrade-failed", "技能未绑定技能树节点")
            return
        }
        open(player, route, target.tree, target.node)
    }

    fun open(player: Player, skill: PlayerSkill) {
        open(player, skill.immutable)
    }

    private fun open(player: Player, route: PlayerRoute, tree: ImmutableSkillTree, node: SkillTreeSkillNode) {
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, node.skillId)
        val immutableSkill = route.getImmutableSkill(node.skillId)
        if (immutableSkill == null) {
            player.sendLang("skill-upgrade-failed", "技能不存在")
            return
        }
        val item = DynamicSkillIcon.build(player, immutableSkill, level)
        BaseUI.createBaseUI {
            BaseUI.chest(this@PlayerSkillUpgradeUI) {
                onBuild { _, inventory ->
                    setDecorateIcon(decorateIcon.get(), inventory)
                }
                set(22, item) {
                    advance(player, route, tree, node)
                }
            }
        }.openTo(player)
    }

    private fun advance(player: Player, route: PlayerRoute, tree: ImmutableSkillTree, node: SkillTreeSkillNode) {
        try {
            route.advanceNode(player, tree.id, node.id).thenAccept {
                val immutableSkill = route.getImmutableSkill(node.skillId)
                if (immutableSkill != null) {
                    open(player, immutableSkill)
                }
            }.exceptionally { exception ->
                val message = exception.message ?: "Unknown error"
                player.sendLang("skill-upgrade-failed", message)
                null
            }
        } catch (exception: Exception) {
            val message = exception.message ?: "Unknown error"
            player.sendLang("skill-upgrade-failed", message)
        }
    }

    private fun findSkillNode(route: PlayerRoute, skillId: String): NodeTarget? {
        for (tree in route.skillTrees) {
            for (node in tree.nodes.values) {
                if (node is SkillTreeSkillNode && node.skillId == skillId) {
                    return NodeTarget(tree, node)
                }
            }
        }
        return null
    }

    private class NodeTarget(
        val tree: ImmutableSkillTree,
        val node: SkillTreeSkillNode
    )
}
