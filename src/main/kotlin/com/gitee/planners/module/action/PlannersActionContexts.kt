package com.gitee.planners.module.action

import com.gitee.action.api.Action
import com.gitee.action.api.model.ActionContext
import com.gitee.action.api.scope.AttributeBagImpl
import com.gitee.action.api.spi.HostServices
import com.gitee.planners.module.action.query.QueryScope

/**
 * 技能动作上下文的装配入口。
 *
 * 技能执行上下文整体放入类型袋；触发者与来源目标取自作用域的施法者与当前目标，
 * 因此 `@sender` 与 `@this` 在技能语义下指向正确的对象。
 */
object PlannersActionContexts {

    /**
     * 构造执行上下文。
     *
     * @param scope 本次技能的查询作用域
     * @param host 宿主服务
     * @return 执行上下文
     */
    fun of(scope: QueryScope, host: HostServices): ActionContext {
        val attributes = AttributeBagImpl.builder()
            .put(PlannersActionAttributes.SCOPE, scope)
            .build()
        // 施法者即触发者：与技能侧 sender 目标器取同一对象，避免两条派发路径给出不同结果
        val actor = PlannersActionTargets.toAction(scope.caster ?: scope.sender)
        val origin = PlannersActionTargets.toAction(scope.target)
        return ActionContext(
            Action.scopes().require(PlannersActionScopes.SKILL),
            host,
            actor,
            origin,
            attributes,
            emptyMap<String, Any>(),
            0,
        )
    }

    /**
     * 从执行上下文取回技能查询作用域。
     *
     * @param context Action 执行上下文
     * @return 技能查询作用域
     * @throws IllegalStateException 类型袋缺少作用域时抛出
     */
    fun scopeOf(context: ActionContext): QueryScope {
        val scope = context.getAttributes().get(PlannersActionAttributes.SCOPE)
        if (scope == null) {
            throw IllegalStateException("planners action requires a skill scope")
        }
        return scope
    }
}
