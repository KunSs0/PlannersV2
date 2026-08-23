package com.gitee.planners.core.skilltree

import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeNode
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import com.gitee.planners.core.player.PlayerTemplate

class SkillTreeNodeProviderContext(
    val template: PlayerTemplate,
    val route: PlayerRoute,
    val tree: ImmutableSkillTree,
    val node: SkillTreeNode,
    val state: PlayerSkillTreeNodeState
)
