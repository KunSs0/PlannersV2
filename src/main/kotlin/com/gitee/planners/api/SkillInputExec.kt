package com.gitee.planners.api

import com.gitee.planners.api.pick.PickResult
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
     * 指向性技能在构造时就携带已确认的指向结果，Hook 通过 [pick] 读取本次施放的
     * 目标或落点快照；两种恢复方式只在是否替换该结果上有差异。
     *
     * @property player 本次施放者。
     * @property skill 当前玩家技能。
     * @property pick 本次施放已确认的指向结果；非指向技能为 null。
     * @param continuation 完成Hook后进入最终校验与资源提交的操作。
     */
    class Context(
        val player: Player,
        val skill: PlayerSkill,
        val pick: PickResult?,
        private val continuation: (PickResult?) -> ExecutableResult
    ) {
        private val resumed = AtomicBoolean(false)

        /**
         * 恢复本次释放且只允许执行一次。
         *
         * 使用构造时携带的指向结果继续，使 Hook 不参与指向时 `@target` 仍解析到本次确认的目标或落点。
         *
         * @return 最终释放结果。
         * @throws IllegalStateException Hook重复恢复同一次释放时抛出。
         */
        fun resume(): ExecutableResult {
            // 多个动画或输入回调不能重复提交同一份冷却和资源。
            check(resumed.compareAndSet(false, true)) { "同一次技能释放只能恢复一次。" }
            // 不额外传入结果时沿用施放确认时捕获的快照。
            return continuation(pick)
        }

        /**
         * 恢复技能执行并提供指向性结果。
         *
         * 调用方传入的结果替换施放时捕获的快照，后续 `@target` 与 `skill.pick` 都以它为准。
         *
         * @param pick 已确认的指向性结果。
         * @return 当前释放结果。
         */
        fun resume(pick: PickResult): ExecutableResult {
            // 与无指向版本共享同一个消费标记。
            check(resumed.compareAndSet(false, true)) { "同一次技能释放只能恢复一次。" }
            // 显式传入的结果优先，供 Hook 校验后替换目标或落点。
            return continuation(pick)
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
