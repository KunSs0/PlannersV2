package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * 只对持有实体句柄的目标求值的规则基类。
 *
 * 位置目标（世界坐标）没有实体句柄，统一在这里被过滤掉，不需要每条规则各自判空；
 * 规则实现只处理实体本身，并自行决定存活、生命与阵营判据。
 */
abstract class EntityFilterRule : FilterRule {

    /**
     * 取出实体句柄后交给 [matches]。
     *
     * @param scope 当前执行上下文。
     * @param target 候选目标。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 保留时返回 true；目标没有实体句柄时返回 false。
     */
    final override fun test(scope: QueryScope, target: ActionTarget, arguments: ActionArguments, compiled: Any?): Boolean {
        val entity = target.bukkitEntity()
        if (entity == null) {
            return false
        }
        return matches(scope, target, entity, arguments, compiled)
    }

    /**
     * 按实体判据决定是否保留。
     *
     * @param scope 当前执行上下文。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 保留时返回 true。
     */
    protected abstract fun matches(
        scope: QueryScope,
        target: ActionTarget,
        entity: LivingEntity,
        arguments: ActionArguments,
        compiled: Any?,
    ): Boolean
}
