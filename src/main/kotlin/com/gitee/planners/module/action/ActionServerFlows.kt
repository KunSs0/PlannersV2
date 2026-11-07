package com.gitee.planners.module.action

import taboolib.common.platform.function.warning

/**
 * 技能加载期流程的执行入口。
 *
 * `server` 流程用于声明「技能被加载时要做的事」，例如注册定时器或建立缓存。
 * 它在技能注册完成后执行一次，此时没有玩家上下文，因此依赖玩家或目标实体的
 * 条目在解析阶段就会被跳过；确实需要玩家数据的动作应放到事件流程里。
 */
object ActionServerFlows {

    /**
     * 执行全部技能的 `server` 流程。
     *
     * @param specs 已编译的技能结构。
     * @return 实际执行动作行数的总和。
     */
    fun runAll(specs: Collection<ActionSpec>): Int {
        var executed = 0
        for (spec in specs) {
            executed += runFor(spec)
        }
        return executed
    }

    /**
     * 执行单个技能的 `server` 流程。
     *
     * `server` 流程是普通流程：立即动作按顺序执行，带触发器的行跳过。
     *
     * @param spec 技能结构。
     * @return 实际执行的动作行数。
     */
    fun runFor(spec: ActionSpec): Int {
        if (!spec.hasFlow(ActionSpec.SERVER)) {
            return 0
        }
        val scope = ActionScopeFactory.forSystem("skill:" + spec.skillId + ".server")
        return try {
            ActionRunner.run(spec, ActionSpec.SERVER, scope)
        } catch (exception: Exception) {
            warning(
                "Action server flow failed in skill '" + spec.skillId + "': " + exception.message,
            )
            0
        }
    }
}
