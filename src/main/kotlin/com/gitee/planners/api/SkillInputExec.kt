package com.gitee.planners.api

import com.gitee.planners.api.directing.DirectingResult
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 技能输入执行上下文。
 *
 * 持有技能释放的后续执行权，
 * 由 [SkillInputExecHook] 接管后在合适的时机调用 [Context.resume] 继续。
 */
class SkillInputExec private constructor() {

    /**
     * 动画或输入Hook持有的一次性释放续体。
     *
     * @property player 本次施放者。
     * @property skill 当前玩家技能。
     * @param continuation 完成Hook后进入最终校验与资源提交的操作。
     */
    class Context(
        val player: Player,
        val skill: PlayerSkill,
        private val continuation: (DirectingResult?) -> ExecutableResult
    ) {
        private val resumed = AtomicBoolean(false)

        /**
         * 恢复本次释放且只允许执行一次。
         *
         * @return 最终释放结果。
         * @throws IllegalStateException Hook重复恢复同一次释放时抛出。
         */
        fun resume(): ExecutableResult {
            // 多个动画或输入回调不能重复提交同一份冷却和资源。
            check(resumed.compareAndSet(false, true)) { "同一次技能释放只能恢复一次。" }
            return continuation(null)
        }

        /**
         * 恢复技能执行并提供指向性结果。
         *
         * @param directing 已确认的指向性结果。
         * @return 当前释放结果。
         */
        fun resume(directing: DirectingResult): ExecutableResult {
            // 与无指向版本共享同一个消费标记。
            check(resumed.compareAndSet(false, true)) { "同一次技能释放只能恢复一次。" }
            return continuation(directing)
        }

        /**
         * 放弃本次释放，不再执行最终校验、资源提交和技能脚本。
         *
         * 与 [resume] 共享同一个一次性标记；两者只能有一个生效。
         */
        fun cancel() {
            // 客户端拒绝或播放请求失效时，不能把未执行的技能继续交给最终提交链。
            check(resumed.compareAndSet(false, true)) { "同一次技能释放只能恢复或取消一次。" }
        }
    }
}
