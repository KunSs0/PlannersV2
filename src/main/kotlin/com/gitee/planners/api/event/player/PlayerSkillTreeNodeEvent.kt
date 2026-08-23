package com.gitee.planners.api.event.player

import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeNode
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

class PlayerSkillTreeNodeEvent(
    val template: PlayerTemplate,
    val route: PlayerRoute,
    val tree: ImmutableSkillTree,
    val node: SkillTreeNode,
    val from: Int,
    val to: Int
) : BukkitProxyEvent() {

    val player = template.onlinePlayer
}
