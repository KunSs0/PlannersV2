package com.gitee.planners.core.skill.status

import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * 技能状态检查事件。
 *
 * 宿主算完内置条件后派发，业务可以在这里补充环境型结论，
 * 例如"交战中且附近有敌对单位""场景不允许释放"。
 *
 * @property player 玩家。
 * @property skills 本次参与检查的已装备技能。
 * @property results 技能 ID 到检查结果；业务可以覆写或追加。
 */
class PlayerSkillStatusCheckEvent(
    val player: Player,
    val skills: List<PlayerSkill>,
    val results: MutableMap<String, PlayerSkillStatus.Status>,
) : BukkitProxyEvent()
