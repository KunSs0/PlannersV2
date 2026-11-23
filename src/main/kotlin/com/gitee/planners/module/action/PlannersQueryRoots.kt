package com.gitee.planners.module.action

import com.gitee.action.api.Action
import com.gitee.action.api.model.ActionContext
import com.gitee.action.api.spi.ValueRoot
import com.gitee.planners.module.action.query.QueryPath
import com.gitee.planners.module.action.query.QueryRoot

/**
 * 技能侧取值根。
 *
 * 表达式由 Action 统一求值，本类只负责"某个根取什么值"：把根关键字与剩余分段交给
 * [QueryPath] 的分段语义处理，再作用于本次技能的 [com.gitee.planners.module.action.query.QueryScope]。
 *
 * 这样字段解析规则（例如 `data` 的剩余段整体作为 key、`node` 必须是树与节点两段）保持原样，
 * 而表达式解析与求值不再由技能侧实现。
 */
object PlannersQueryRoots {

    /**
     * 注册全部技能取值根。
     *
     * @param owner 注册方，用于重载时撤销
     */
    fun install(owner: Any) {
        for (root in QueryRoot.values()) {
            // 重复安装（重载或测试）时跳过已存在的根
            if (Action.values().get(root.id) != null) {
                continue
            }
            Action.values().register(RootBridge(root), owner)
        }
    }

    /**
     * 单个根的桥接实现。
     *
     * @property root 根节点
     */
    private class RootBridge(private val root: QueryRoot) : ValueRoot {

        override fun getId(): String {
            return root.id
        }

        override fun resolve(path: String, context: ActionContext): Any? {
            val scope = PlannersActionContexts.scopeOf(context)
            val text = if (path.isEmpty()) {
                root.id
            } else {
                root.id + "." + path
            }
            // 复用既有解析：分段数量与字段合法性在那里统一校验
            return QueryPath.parse(text, "expression").resolve(scope)
        }
    }
}
