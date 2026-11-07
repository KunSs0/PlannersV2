package com.gitee.planners.module.action

/**
 * Planners 自身事件对应的触发器声明。
 *
 * 触发器只提供标识与别名；匹配由 [ActionTriggerIndex] 按 id 完成，
 * Bukkit 监听与派发由宿主桥接层负责。
 */
object ActionTriggers {

    /** 技能进入施法流程。 */
    object SkillCast : ActionTrigger {

        override val id = "on_skill_cast"

        override val aliases = listOf("on_skill_cast")
    }

    /** 技能冷却被设置。 */
    object SkillCooldown : ActionTrigger {

        override val id = "on_skill_cooldown"

        override val aliases = listOf("on_skill_cooldown")
    }

    /** 玩家首次学会技能。 */
    object SkillLearn : ActionTrigger {

        override val id = "on_skill_learn"

        override val aliases = listOf("on_skill_learn")
    }

    /** 技能等级变化。 */
    object SkillLevel : ActionTrigger {

        override val id = "on_skill_level"

        override val aliases = listOf("on_skill_level")
    }

    /** 技能树节点变化。 */
    object SkillNode : ActionTrigger {

        override val id = "on_skill_node"

        override val aliases = listOf("on_skill_node")
    }

    /** 角色等级变化。 */
    object Level : ActionTrigger {

        override val id = "on_level"

        override val aliases = listOf("on_level")
    }

    /** 经验变化。 */
    object Experience : ActionTrigger {

        override val id = "on_experience"

        override val aliases = listOf("on_experience")
    }

    /** 法力值变化。 */
    object MagicPoint : ActionTrigger {

        override val id = "on_magic_point"

        override val aliases = listOf("on_magic_point")
    }

    /** 捕获目标。 */
    object TargetCaptured : ActionTrigger {

        override val id = "on_target_captured"

        override val aliases = listOf("on_target_captured")
    }

    /** 玩家对实体造成伤害。 */
    object PlayerDamageEntity : ActionTrigger {

        override val id = "on_player_damage_entity"

        override val aliases = listOf("on_player_damage_entity")
    }

    /** 职业被清除。 */
    object RouteCleared : ActionTrigger {

        override val id = "on_route_cleared"

        override val aliases = listOf("on_route_cleared")
    }

    /** 玩家退出。 */
    object Quit : ActionTrigger {

        override val id = "on_quit"

        override val aliases = listOf("on_quit")
    }
}
