package com.gitee.planners.api

import com.gitee.planners.api.directing.DirectingResult
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
        private val continuation: (DirectingResult?) -> ExecutableResult
    ) {
        fun resume(): ExecutableResult {
            return continuation(null)
        }

        /**
         * 恢复技能执行并提供指向性结果。
         *
         * @param directing 已确认的指向性结果。
         * @return 当前释放结果。
         */
        fun resume(directing: DirectingResult): ExecutableResult {
            return continuation(directing)
        }
    }
}
