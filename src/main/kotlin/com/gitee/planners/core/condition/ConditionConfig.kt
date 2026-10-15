package com.gitee.planners.core.condition

import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 不可变条件模板，异步准备与同步执行分离。
 * @property key 配置引用 ID。
 * @property props 默认参数，保留既有配置值边界。
 * @property hint 未满足条件时的提示模板。
 * @property consume Nova 消耗源码；原生只读条件为空。
 */
abstract class ConditionConfig(
    val key: String,
    val props: Map<String, Any>,
    val hint: String,
    val consume: String?
) {
    /** 参数是否作为 Nova 表达式预编译和计算。 */
    abstract val expressionProperties: Boolean
    /** 同一准备键在一次请求内只读取一次外部数据。 */
    abstract val preparationKey: String
    /** 登记模板源码；原生条件没有源码。 */
    abstract fun registerSources()
    /** 异步读取执行数据，返回本次请求专属执行器。 */
    abstract fun prepare(playerId: UUID): CompletableFuture<ConditionExecution>
    /** 配置加载期验证参数；脚本参数在运行期计算。 */
    open fun validateProperties(properties: Map<String, Any>) {
    }
}
