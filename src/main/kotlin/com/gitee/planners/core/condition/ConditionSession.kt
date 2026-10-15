package com.gitee.planners.core.condition

import java.util.Collections

/** 一次技能树展示或学习请求的条件数据快照，不跨请求缓存玩家数据。 */
class ConditionSession(executions: Map<String, ConditionExecution>) {
    private val executions = Collections.unmodifiableMap(LinkedHashMap(executions))

    /** 获取已准备的模板；禁止执行过程中临时查询外部服务。 */
    fun execution(key: String): ConditionExecution {
        val execution = executions[key]
        if (execution == null) {
            error("条件尚未准备：$key")
        }
        return execution
    }
}
