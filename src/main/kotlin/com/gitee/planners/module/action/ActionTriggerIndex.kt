package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import taboolib.common.platform.function.warning

/**
 * 触发器索引。
 *
 * 加载期把所有技能的 Action 结构按触发器主键归并；运行期一次事件只需按
 * 主键取出相关技能，再逐个派发，避免每次遍历全部技能。
 *
 * 索引不可变：重载时整体重建。
 */
class ActionTriggerIndex(private val entries: Map<String, List<ActionSpec>>) {

    /**
     * 判断是否存在声明该触发器的技能。
     *
     * @param triggerId 触发器主键。
     * @return 存在时返回 true。
     */
    fun hasTrigger(triggerId: String): Boolean {
        return entries.containsKey(triggerId)
    }

    /**
     * 取声明该触发器的技能列表。
     *
     * @param triggerId 触发器主键。
     * @return 技能结构列表；没有声明时返回空列表。
     */
    fun specsFor(triggerId: String): List<ActionSpec> {
        val value = entries[triggerId]
        if (value == null) {
            return emptyList()
        }
        return value
    }

    /**
     * 派发一次事件。
     *
     * 每个技能独立求值自己的流程条件；单个技能抛错不影响其他技能。
     *
     * @param triggerId 触发器主键。
     * @param scopeFactory 按技能生成执行上下文的工厂。
     * @return 实际执行的动作行数。
     */
    fun dispatch(triggerId: String, scopeFactory: (ActionSpec) -> QueryScope): Int {
        val specs = specsFor(triggerId)
        if (specs.isEmpty()) {
            return 0
        }
        var executed = 0
        for (spec in specs) {
            try {
                executed += ActionRunner.fire(spec, triggerId, scopeFactory(spec))
            } catch (exception: Exception) {
                warning(
                    "Action trigger '$triggerId' failed in skill '" + spec.skillId + "': " +
                        exception.message,
                )
            }
        }
        return executed
    }

    /**
     * 返回全部触发器主键，用于诊断输出。
     *
     * @return 主键集合。
     */
    fun triggerIds(): Set<String> {
        return entries.keys
    }

    companion object {

        /**
         * 从全部技能结构构建索引。
         *
         * @param specs 已编译的技能结构。
         * @return 不可变索引。
         */
        fun build(specs: Collection<ActionSpec>): ActionTriggerIndex {
            val result = LinkedHashMap<String, MutableList<ActionSpec>>()
            for (spec in specs) {
                val ids = collectTriggerIds(spec)
                for (id in ids) {
                    val list = result[id]
                    if (list == null) {
                        result[id] = mutableListOf(spec)
                    } else {
                        list.add(spec)
                    }
                }
            }
            val frozen = LinkedHashMap<String, List<ActionSpec>>()
            for (entry in result.entries) {
                frozen[entry.key] = entry.value.toList()
            }
            return ActionTriggerIndex(frozen)
        }

        /**
         * 收集单个技能声明的全部触发器主键。
         *
         * @param spec 技能结构。
         * @return 触发器主键集合。
         */
        private fun collectTriggerIds(spec: ActionSpec): Set<String> {
            val result = LinkedHashSet<String>()
            for (flow in spec.flows.values) {
                for (line in flow.actions) {
                    val trigger = line.trigger
                    if (trigger == null) {
                        continue
                    }
                    if (line.isTimer()) {
                        continue
                    }
                    result.add(trigger.id)
                }
            }
            return result
        }
    }
}
