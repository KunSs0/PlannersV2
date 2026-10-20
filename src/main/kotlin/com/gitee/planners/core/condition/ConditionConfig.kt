package com.gitee.planners.core.condition

import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 不可变条件模板，异步准备与同步执行分离。
 * @property key 配置引用 ID。
 * @property props 默认参数，保留既有配置值边界。
 * @property hint 未满足条件时的提示模板。
 * @property consumable 是否在学习成功提交前创建内部成本句柄。
 */
abstract class ConditionConfig(
    val key: String,
    val props: Map<String, Any>,
    val hint: String,
    val consumable: Boolean
) {
    /** 同一准备键在一次请求内只读取一次外部数据。 */
    abstract val preparationKey: String
    /** 异步读取内部数据，返回本次请求专属执行器。 */
    abstract fun prepare(playerId: UUID): CompletableFuture<ConditionExecution>
    /** 配置加载期验证参数；脚本参数在运行期计算。 */
    open fun validateProperties(properties: Map<String, Any>) {
    }
}
