package com.gitee.planners.api

/**
 * 技能输入执行钩子。
 *
 * 实现此接口的 [com.gitee.planners.core.config.ImmutableSkill.Hook]
 * 可在技能 Pre 通过后接管后续执行流程。
 *
 * [SkillInputExec.Context.resume] 在适当时机调用以继续 execute + Post。
 */
interface SkillInputExecHook {
    fun intercept(ctx: SkillInputExec.Context)
}
