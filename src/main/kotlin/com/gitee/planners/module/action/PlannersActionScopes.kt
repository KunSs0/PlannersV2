package com.gitee.planners.module.action

import com.gitee.action.api.Action
import com.gitee.action.api.policy.ActionProfile
import com.gitee.action.api.policy.UnknownIdPolicy
import com.gitee.action.api.scope.BasicScope

/**
 * 技能动作作用域。
 *
 * 一个作用域承载全部技能动作：目标器链、触发派发与条件判断都在同一域内，
 * 因此策略一次性覆盖深度、空目标、未知条目与触发器开关。
 */
object PlannersActionScopes {

    /** 作用域标识。 */
    const val SKILL = "planners:skill"

    /**
     * 注册作用域。
     *
     * @param owner 注册方，通常是插件实例
     */
    fun install(owner: Any) {
        val scopes = Action.scopes()
        if (scopes.get(SKILL) != null) {
            return
        }
        scopes.register(
            BasicScope.builder(SKILL)
                .owner("planners")
                .profile(profile())
                .build()
        )
    }

    /**
     * 技能域策略。
     *
     * 深度沿用技能历史的 16，空目标即跳过本行，外部插件晚于本插件注册的动作先留延迟绑定，
     * 命名流程允许由宿主按名直调。
     *
     * @return 策略
     */
    private fun profile(): ActionProfile {
        return ActionProfile.builder()
            .maxDepth(16)
            .emptyTargets(com.gitee.action.api.policy.EmptyTargets.SKIP_LINE)
            .defaultTargeter("sender")
            .allowUnreachableFlows(true)
            .unknownId(UnknownIdPolicy.DEFER)
            .build()
    }
}
