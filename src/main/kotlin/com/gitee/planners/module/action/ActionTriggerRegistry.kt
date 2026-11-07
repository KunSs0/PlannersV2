package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 触发器索引的持有者。
 *
 * 技能加载完成后整体重建索引；重载时先关闭旧代际再重建，
 * 因此运行期读到的始终是当前代际的索引。
 */
object ActionTriggerRegistry {

    @Volatile
    private var current = ActionTriggerIndex.build(emptyList())

    /**
     * 用新的技能结构重建索引。
     *
     * @param specs 已编译的技能结构。
     */
    fun rebuild(specs: Collection<ActionSpec>) {
        current = ActionTriggerIndex.build(specs)
    }

    /**
     * 清空索引，供插件停用使用。
     */
    fun clear() {
        current = ActionTriggerIndex.build(emptyList())
    }

    /**
     * 取当前索引。
     *
     * @return 索引快照。
     */
    fun index(): ActionTriggerIndex {
        return current
    }

    /**
     * 派发一次事件。
     *
     * @param triggerId 触发器主键。
     * @param scopeFactory 按技能生成执行上下文的工厂。
     * @return 实际执行的动作行数。
     */
    fun dispatch(triggerId: String, scopeFactory: (ActionSpec) -> QueryScope): Int {
        return current.dispatch(triggerId, scopeFactory)
    }

    /**
     * 判断是否存在声明该触发器的技能。
     *
     * @param triggerId 触发器主键。
     * @return 存在时返回 true。
     */
    fun hasTrigger(triggerId: String): Boolean {
        return current.hasTrigger(triggerId)
    }
}
