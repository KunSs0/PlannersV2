package com.gitee.planners.core.condition

import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 条件执行器。
 * 集中定义在 config.yml 的条件模板通过 key 引用 + 传参覆盖后，由本类执行。
 * @param configurations 本次配置查询入口；异步准备只解析实际引用的模板。
 */
class ConditionEvaluator(
    private val configurations: () -> Map<String, ConditionConfig> = { Planners.conditions.get() }
) {

    companion object {

        /** 原生条件不需要预编译配置属性；保留空入口以便重载流程保持顺序稳定。 */
        fun prepareConfiguredProperties(configurations: Collection<ConditionConfig>) {
        }

        /** 每次 Workspace 重建前清除上一代函数句柄，避免旧模块 ID 被复用。 */
        fun resetConfiguredProperties() {
        }
    }

    /**
     * 准备本次请求实际引用的条件，同一数据源只调用一次。
     * 不在此方法访问玩家的 Bukkit 状态，所有外部查询异常原样传播。
     */
    fun prepare(keys: Set<String>, playerId: UUID): CompletableFuture<ConditionSession> {
        val sources = LinkedHashMap<String, CompletableFuture<ConditionExecution>>()
        val executions = LinkedHashMap<String, CompletableFuture<ConditionExecution>>()
        try {
            val templates = configurations()
            for (key in keys) {
                val config = templates[key]
                require(config != null) { "未知条件：$key" }
                var future = sources[config.preparationKey]
                if (future == null) {
                    future = config.prepare(playerId)
                    sources[config.preparationKey] = future
                }
                executions[key] = future
            }
        } catch (exception: Exception) {
            return CompletableFuture.failedFuture(exception)
        }
        return CompletableFuture.allOf(*sources.values.toTypedArray()).thenApply {
            val prepared = LinkedHashMap<String, ConditionExecution>()
            for ((key, future) in executions) {
                // allOf 成功后所有查询已结束，此处不会阻塞。
                prepared[key] = future.join()
            }
            ConditionSession(prepared)
        }
    }

    /**
     * 为同步展示接口准备数据，仅允许后台快照线程等待尚未结束的查询。
     * 主线程调用含异步条件的接口会明确报错，应改用先 prepare 后执行。
     */
    fun prepareBlocking(keys: Set<String>, player: Player): ConditionSession {
        val future = prepare(keys, player.uniqueId)
        check(future.isDone || !Bukkit.isPrimaryThread()) { "主线程不能等待异步条件，请先 prepare 条件数据" }
        return future.join()
    }

    /**
     * 批量校验的单个条件请求。
     *
     * @property id 调用方用于关联结果的唯一标识。
     * @property conditions 本次需要校验的条件组。
     * @property contextVars 本次条件可使用的附加上下文变量。
     */
    data class VerifyRequest(val id: String, val conditions: Map<String, Map<String, Any>>, val contextVars: Map<String, Any>)

    data class VerifyResult(
        val passed: Boolean,
        val hints: List<String>
    ) {
        companion object {
            val PASSED = VerifyResult(true, emptyList())
        }
    }

    /**
     * 校验条件组。
     *
     * @param conditions 条件引用：key → override props
     * @param player     Bukkit Player
     * @param contextVars 额外上下文变量（供 props 公式引用）
     */
    fun verify(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        contextVars: Map<String, Any> = emptyMap(),
        session: ConditionSession? = null
    ): VerifyResult {
        val conditionConfigs = collectConditionConfigs(listOf(VerifyRequest("verify", conditions, contextVars)))
        val prepared = session ?: prepareBlocking(conditions.keys, player)
        return verifyInternal(conditions, player, contextVars, prepared)
    }

    /**
     * 批量条件校验结果及其请求级性能统计。
     *
     * @property results 请求 ID 到校验结果的映射。
     * @property profiling 本批条件校验的纳秒级统计。
     */
    class BatchVerification(
        val results: Map<String, VerifyResult>,
        val profiling: BatchProfiling
    ) {
    }

    /**
     * 技能树批量条件校验的分段统计。
     *
     * 所有值均以纳秒累加；调用方应在一个完整请求结束后再换算为微秒输出。
     */
    class BatchProfiling {

        var requestPreparationNanos: Long = 0L
        var propertyResolveNanos: Long = 0L
        var propertyExpressionNanos: Long = 0L
        var groupInputBuildNanos: Long = 0L
        var conditionInvokeNanos: Long = 0L
        var resultApplyNanos: Long = 0L
        var propertyExpressionCount: Int = 0
        var conditionInvokeCount: Int = 0
        var conditionInvokeFailureCount: Int = 0
        var firstConditionInvokeFailure: String? = null

        private val groups = LinkedHashMap<String, GroupProfiling>()

        /** 累加一次属性表达式的预编译纯函数调用耗时。 */
        /** 记录批处理调用在脚本执行前后抛出的异常。 */
        fun recordConditionInvokeFailure(exception: Exception) {
            conditionInvokeFailureCount += 1
            if (firstConditionInvokeFailure == null) {
                firstConditionInvokeFailure = exception.javaClass.name + ":" + exception.message
            }
        }

        /** 将统计压缩为单条适合服务器日志的微秒文本。 */
        fun toLogText(): String {
            val text = StringBuilder()
            text.append("conditionUs={prepare=")
            text.append(formatMicros(requestPreparationNanos))
            text.append(",props=")
            text.append(formatMicros(propertyResolveNanos))
            text.append(",propExpr=")
            text.append(formatMicros(propertyExpressionNanos))
            text.append("/")
            text.append(propertyExpressionCount)
            text.append(",batchInput=")
            text.append(formatMicros(groupInputBuildNanos))
            text.append(",batchInvoke=")
            text.append(formatMicros(conditionInvokeNanos))
            text.append("/")
            text.append(conditionInvokeCount)
            text.append(",fail=")
            text.append(conditionInvokeFailureCount)
            text.append(",apply=")
            text.append(formatMicros(resultApplyNanos))
            text.append("}")
            if (firstConditionInvokeFailure != null) {
                text.append(" error=")
                text.append(firstConditionInvokeFailure)
            }
            if (groups.isNotEmpty()) {
                text.append(" groups={")
                var first = true
                for ((key, group) in groups) {
                    if (!first) {
                        text.append(",")
                    }
                    text.append(key)
                    text.append(":inputs=")
                    text.append(group.inputCount)
                    text.append(",calls=")
                    text.append(group.invokeCount)
                    text.append(",totalUs=")
                    text.append(formatMicros(group.totalNanos))
                    first = false
                }
                text.append("}")
            }
            return text.toString()
        }

        private fun formatMicros(nanos: Long): String {
            return (nanos / 1_000L).toString()
        }

        private class GroupProfiling {

            var inputCount: Int = 0
            var invokeCount: Int = 0
            var totalNanos: Long = 0L
        }
    }

    /**
     * 在同一预编译 Workspace 中批量校验多个条件组。
     *
     * 每个请求保持与 [verify] 一致的条件顺序和失败短路结果，
     * 脚本计算按条件定义分组批量执行，避免节点数量线性增加 Nova 调用次数。
     *
     * @param requests 待校验请求列表。
     * @param player 当前玩家。
     * @return 请求 ID 到校验结果的映射。
     */
    fun verifyAll(requests: List<VerifyRequest>, player: Player): Map<String, VerifyResult> {
        return verifyAllProfiled(requests, player).results
    }

    /**
     * 在同一预编译 Workspace 中批量校验多个条件组，并返回请求级细分计时。
     *
     * @param requests 待校验请求列表。
     * @param player 当前玩家。
     * @return 条件校验结果与分段性能统计。
     */
    fun verifyAllProfiled(requests: List<VerifyRequest>, player: Player, session: ConditionSession? = null): BatchVerification {
        val result = LinkedHashMap<String, VerifyResult>()
        val profiling = BatchProfiling()
        if (requests.isEmpty()) {
            return BatchVerification(result, profiling)
        }
        val keys = LinkedHashSet<String>()
        for (request in requests) {
            keys.addAll(request.conditions.keys)
        }
        val preparedSession = session ?: prepareBlocking(keys, player)
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null
        val preparedRequests = ArrayList<MutableList<PreparedCondition>>()
        val groupedConditions = LinkedHashMap<String, MutableList<PreparedCondition>>()
        for (requestIndex in requests.indices) {
            val requestPrepareStart = System.nanoTime()
            val request = requests[requestIndex]
            val prepared = ArrayList<PreparedCondition>()
            preparedRequests.add(prepared)
            for ((key, overrideProps) in request.conditions) {
                val config = configurations()[key]
                if (config == null) {
                    error("Unknown condition key: $key")
                }
                val resolveStart = System.nanoTime()
                val resolvedProps = resolveProps(
                    config,
                    overrideProps,
                    player,
                    profile,
                    router,
                    route,
                    request.contextVars,
                    profiling
                )
                profiling.propertyResolveNanos += System.nanoTime() - resolveStart
                val preparedCondition = PreparedCondition(key, config, resolvedProps.values, request.contextVars)
                prepared.add(preparedCondition)
                var group = groupedConditions[key]
                if (group == null) {
                    group = ArrayList()
                    groupedConditions[key] = group
                }
                group.add(preparedCondition)
            }
            profiling.requestPreparationNanos += System.nanoTime() - requestPrepareStart
        }
        for ((key, group) in groupedConditions) {
            val inputBuildStart = System.nanoTime()
            val inputs = ArrayList<Map<String, Any>>()
            for (preparedCondition in group) {
                val input = LinkedHashMap<String, Any>()
                input["props"] = preparedCondition.props
                input.putAll(preparedCondition.contextVars)
                inputs.add(input)
            }
            profiling.groupInputBuildNanos += System.nanoTime() - inputBuildStart
            val invocationStart = System.nanoTime()
            val evaluation: ConditionExecution.BatchEvaluation
            try {
                evaluation = preparedSession.execution(key).evaluateBatchPersistent(player, profile, router, route, inputs)
            } catch (exception: Exception) {
                profiling.recordConditionInvokeFailure(exception)
                throw IllegalStateException("Failed to evaluate condition batch: $key", exception)
            }
            profiling.conditionInvokeNanos += System.nanoTime() - invocationStart
            val encoded = evaluation.encoded
            val resultApplyStart = System.nanoTime()
            if (encoded.length != group.size) {
                throw IllegalStateException("Condition batch returned an invalid result length: $key")
            } else {
                for (index in group.indices) {
                    group[index].passed = encoded[index] == '1'
                }
            }
            profiling.resultApplyNanos += System.nanoTime() - resultApplyStart
        }
        val resultBuildStart = System.nanoTime()
        for (requestIndex in requests.indices) {
            val request = requests[requestIndex]
            val prepared = preparedRequests[requestIndex]
            var verification = VerifyResult.PASSED
            for (preparedCondition in prepared) {
                if (!preparedCondition.passed) {
                    verification = VerifyResult(
                        false,
                        listOf(interpolate(preparedCondition.config.hint, preparedCondition.props))
                    )
                    break
                }
            }
            result[request.id] = verification
        }
        profiling.resultApplyNanos += System.nanoTime() - resultBuildStart
        return BatchVerification(result, profiling)
    }

    private fun verifyInternal(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        contextVars: Map<String, Any>,
        session: ConditionSession
    ): VerifyResult {
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null
        val hints = mutableListOf<String>()
        for ((key, overrideProps) in conditions) {
            val cfg = configurations()[key]
            if (cfg == null) {
                error("Unknown condition key: $key")
            }
            val resolvedProps = resolveProps(cfg, overrideProps, player, profile, router, route, contextVars)
            val props = resolvedProps.values
            val passed = session.execution(key).evaluate(player, profile, router, route, props)
            if (!passed) {
                hints.add(interpolate(cfg.hint, props))
                return VerifyResult(false, hints)
            }
        }
        return VerifyResult.PASSED
    }

    /**
     * 准备可回滚成本（校验通过后调用），准备失败时释放此前所有句柄。
     *
     * @param conditions 条件引用：key → override props
     * @param player     Bukkit Player
     * @param contextVars 额外上下文变量（供 props 公式引用）
     */
    fun prepareConsume(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        contextVars: Map<String, Any> = emptyMap(),
        session: ConditionSession? = null
    ): LearningCosts {
        val preparedSession = session ?: prepareBlocking(conditions.keys, player)
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null

        val costs = ArrayList<LearningCost>()
        try {
            for ((key, overrideProps) in conditions) {
                val cfg = configurations()[key]
                if (cfg == null) {
                    error("Unknown condition key: $key")
                }
                if (!cfg.consumable) {
                    continue
                }
                val resolvedProps = resolveProps(cfg, overrideProps, player, profile, router, route, contextVars)
                val cost = preparedSession.execution(key).prepareConsume(player, profile, router, route, resolvedProps.values)
                check(cost != null) { "消费条件 $key 必须返回 LearningCost" }
                costs.add(cost)
            }
        } catch (failure: Throwable) {
            LearningCosts(costs).rollback(failure)
            throw failure
        }
        return LearningCosts(costs)
    }

    // ---- 内部 ----

    /** 合并条件模板默认参数与节点覆盖参数，并校验类型参数。 */
    private fun resolveProps(
        configuration: ConditionConfig,
        overrideProps: Map<String, Any>,
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        contextVars: Map<String, Any>,
        profiling: BatchProfiling? = null
    ): ResolvedProps {
        val merged = configuration.props.toMutableMap()
        merged.putAll(overrideProps)
        configuration.validateProperties(merged)
        return ResolvedProps(merged)
    }

    /**
     * hint 中的 {props.xxx} 替换为实际值。
     */
    private fun interpolate(hint: String, props: Map<String, Any>): String {
        var result = hint
        for ((key, value) in props) {
            result = result.replace("{props.$key}", value.toString())
        }
        return result
    }

    /**
     * 收集本批请求实际引用的条件。配置 key 已在加载阶段固定，按首次出现顺序安装，
     * 同一个条件即使被多个节点引用也只编译一次。
     */
    private fun collectConditionConfigs(requests: List<VerifyRequest>): List<ConditionConfig> {
        val allConfigs = configurations()
        val result = LinkedHashMap<String, ConditionConfig>()
        for (request in requests) {
            for (key in request.conditions.keys) {
                val config = allConfigs[key]
                if (config == null) {
                    error("Unknown condition key: $key")
                }
                result[key] = config
            }
        }
        return ArrayList(result.values)
    }

    private class ResolvedProps(
        val values: Map<String, Any>
    )

    private class PreparedCondition(
        val key: String,
        val config: ConditionConfig,
        val props: Map<String, Any>,
        val contextVars: Map<String, Any>
    ) {
        var passed: Boolean = false
    }

}
