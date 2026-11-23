package com.gitee.planners.module.action

import com.gitee.action.api.scope.AttributeKey
import com.gitee.planners.module.action.query.QueryScope

/**
 * 技能动作的类型袋键。
 *
 * 技能执行上下文（施法者、目标、技能定义、变量与各字段读取器）整体放入类型袋，
 * 目标器、条件与机制通过键取回自己的业务上下文，Action 的公共面不需要认识技能模型。
 */
object PlannersActionAttributes {

    /** 本次执行的查询作用域，承载技能运行期的全部宿主对象。 */
    val SCOPE: AttributeKey<QueryScope> = AttributeKey("planners.scope")
}
