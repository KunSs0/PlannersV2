package com.gitee.planners.api.directing

import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player

/**
 * 创建指向会话时提供给 provider 的固定上下文。
 *
 * @property sessionId 由 Planners 分配的当前会话 ID。
 * @property player 当前施法者。
 * @property skill 当前玩家技能。
 * @property definition 已在技能加载期完成解析的指向定义。
 * @property sourceKey 触发当前技能的物理按键标识。
 */
class DirectingSessionContext(val sessionId: Long, val player: Player, val skill: PlayerSkill, val definition: DirectingDefinition, val sourceKey: String)
