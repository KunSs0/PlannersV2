package com.gitee.planners.api

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import org.bukkit.entity.Player

/**
 * 技能输入执行上下文。
 *
 * 持有技能释放的后续执行权，
 * 由 [SkillInputExecHook] 接管后在合适的时机调用 [Context.resume] 继续。
 */
class SkillInputExec private constructor() {

    class Context(
        val player: Player,
        val skill: PlayerSkill,
        private val continuation: () -> ExecutableResult
    ) {
        fun resume(): ExecutableResult {
            return continuation()
        }
    }
}
