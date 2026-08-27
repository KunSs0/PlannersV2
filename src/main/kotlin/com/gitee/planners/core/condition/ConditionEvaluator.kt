package com.gitee.planners.core.condition

import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.module.script.NovaScriptUnit
import com.gitee.planners.module.script.NovaSession
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * 条件执行器。
 * 集中定义在 config.yml 的条件模板通过 key 引用 + 传参覆盖后，由本类执行。
 */
class ConditionEvaluator {

    private val propExpressions = ConcurrentHashMap<String, NovaScriptUnit>()

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
        contextVars: Map<String, Any> = emptyMap()
    ): VerifyResult {
        val session = ScriptManager.openReusableSession(ScriptOptions.of(), emptySet())
        try {
            val conditionConfigs = collectConditionConfigs(listOf(VerifyRequest("verify", conditions, contextVars)))
            for (conditionConfig in conditionConfigs) {
                conditionConfig.registerSources()
            }
            return verifyInternal(conditions, player, contextVars, session)
        } finally {
            session.close()
        }
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
        var persistentVariablesCopyNanos: Long = 0L
        var persistentContextSetNanos: Long = 0L
        var persistentArgumentsNanos: Long = 0L
        var persistentContextRestoreNanos: Long = 0L
        var persistentLockWaitNanos: Long = 0L
        var persistentInstallNanos: Long = 0L
        var persistentLookupNanos: Long = 0L
        var persistentExecuteNanos: Long = 0L
        var persistentUnwrapNanos: Long = 0L
        var propertyExpressionCount: Int = 0
        var conditionInvokeCount: Int = 0
        var conditionInvokeFailureCount: Int = 0
        var firstConditionInvokeFailure: String? = null

        private val groups = LinkedHashMap<String, GroupProfiling>()

        /** 累加一次属性表达式调用的宿主与长期会话耗时。 */
        fun recordPropertyInvocation(invocation: ScriptManager.PersistentInvocation) {
            propertyExpressionCount += 1
            recordPersistentInvocation(invocation)
        }

        /** 累加一次按条件 key 批量执行的脚本调用。 */
        fun recordConditionInvocation(key: String, inputCount: Int, invocation: ScriptManager.PersistentInvocation) {
            conditionInvokeCount += 1
            recordPersistentInvocation(invocation)
            var group = groups[key]
            if (group == null) {
                group = GroupProfiling()
                groups[key] = group
            }
            group.inputCount += inputCount
            group.invokeCount += 1
            group.executeNanos += invocation.functionExecuteNanos
            group.totalNanos += totalInvocationNanos(invocation)
        }

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
            text.append(",hostCopy=")
            text.append(formatMicros(persistentVariablesCopyNanos))
            text.append(",hostContext=")
            text.append(formatMicros(persistentContextSetNanos + persistentContextRestoreNanos))
            text.append(",args=")
            text.append(formatMicros(persistentArgumentsNanos))
            text.append(",lock=")
            text.append(formatMicros(persistentLockWaitNanos))
            text.append(",install=")
            text.append(formatMicros(persistentInstallNanos))
            text.append(",lookup=")
            text.append(formatMicros(persistentLookupNanos))
            text.append(",execute=")
            text.append(formatMicros(persistentExecuteNanos))
            text.append(",unwrap=")
            text.append(formatMicros(persistentUnwrapNanos))
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
                    text.append(",executeUs=")
                    text.append(formatMicros(group.executeNanos))
                    first = false
                }
                text.append("}")
            }
            return text.toString()
        }

        private fun recordPersistentInvocation(invocation: ScriptManager.PersistentInvocation) {
            persistentVariablesCopyNanos += invocation.variablesCopyNanos
            persistentContextSetNanos += invocation.contextSetNanos
            persistentArgumentsNanos += invocation.argumentAdaptNanos
            persistentContextRestoreNanos += invocation.contextRestoreNanos
            persistentLockWaitNanos += invocation.lockWaitNanos
            persistentInstallNanos += invocation.installNanos
            persistentLookupNanos += invocation.functionLookupNanos
            persistentExecuteNanos += invocation.functionExecuteNanos
            persistentUnwrapNanos += invocation.resultUnwrapNanos
        }

        private fun totalInvocationNanos(invocation: ScriptManager.PersistentInvocation): Long {
            return invocation.variablesCopyNanos +
                invocation.contextSetNanos +
                invocation.argumentAdaptNanos +
                invocation.contextRestoreNanos +
                invocation.lockWaitNanos +
                invocation.installNanos +
                invocation.functionLookupNanos +
                invocation.functionExecuteNanos +
                invocation.resultUnwrapNanos
        }

        private fun formatMicros(nanos: Long): String {
            return (nanos / 1_000L).toString()
        }

        private class GroupProfiling {

            var inputCount: Int = 0
            var invokeCount: Int = 0
            var totalNanos: Long = 0L
            var executeNanos: Long = 0L
        }
    }

    /**
     * 在同一脚本会话中批量校验多个条件组。
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
     * 在同一脚本会话中批量校验多个条件组，并返回请求级细分计时。
     *
     * @param requests 待校验请求列表。
     * @param player 当前玩家。
     * @return 条件校验结果与分段性能统计。
     */
    fun verifyAllProfiled(requests: List<VerifyRequest>, player: Player): BatchVerification {
        val result = LinkedHashMap<String, VerifyResult>()
        val profiling = BatchProfiling()
        if (requests.isEmpty()) {
            return BatchVerification(result, profiling)
        }
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null
        val options = createOptions(player, profile, router, route)
        val preparedRequests = ArrayList<MutableList<PreparedCondition>>()
        val groupedConditions = LinkedHashMap<String, MutableList<PreparedCondition>>()
        for (requestIndex in requests.indices) {
            val requestPrepareStart = System.nanoTime()
            val request = requests[requestIndex]
            val prepared = ArrayList<PreparedCondition>()
            preparedRequests.add(prepared)
            for ((key, overrideProps) in request.conditions) {
                val config = Planners.conditions.get()[key]
                if (config == null) {
                    error("Unknown condition key: $key")
                }
                val resolveStart = System.nanoTime()
                val resolvedProps = resolveProps(
                    config.props,
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
            val config = group[0].config
            val inputs = ArrayList<Map<String, Any>>()
            for (preparedCondition in group) {
                val input = LinkedHashMap<String, Any>()
                input["props"] = preparedCondition.props
                input.putAll(preparedCondition.contextVars)
                inputs.add(input)
            }
            profiling.groupInputBuildNanos += System.nanoTime() - inputBuildStart
            val invocationStart = System.nanoTime()
            val evaluation: ConditionConfig.BatchEvaluation
            try {
                evaluation = config.evaluateBatchPersistent(options, inputs)
            } catch (exception: Exception) {
                profiling.recordConditionInvokeFailure(exception)
                throw IllegalStateException("Failed to evaluate Nova condition batch: $key", exception)
            }
            profiling.conditionInvokeNanos += System.nanoTime() - invocationStart
            val encoded = evaluation.encoded
            profiling.recordConditionInvocation(key, group.size, evaluation.invocation)
            val resultApplyStart = System.nanoTime()
            if (encoded.length != group.size) {
                throw IllegalStateException("Nova condition batch returned an invalid result length: $key")
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
        session: NovaSession
    ): VerifyResult {
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null
        val hints = mutableListOf<String>()
        val options = createOptions(player, profile, router, route)
        ScriptManager.rebindReusableSession(session, options, setOf("props"))

        for ((key, overrideProps) in conditions) {
            val cfg = Planners.conditions.get()[key]
            if (cfg == null) {
                error("Unknown condition key: $key")
            }
            val resolvedProps = resolveProps(cfg.props, overrideProps, player, profile, router, route, contextVars)
            val props = resolvedProps.values
            options.set("props", props)
            val passed = evalCondition(cfg, session, props)
            if (!passed) {
                hints.add(interpolate(cfg.hint, props))
                return VerifyResult(false, hints)
            }
        }
        return VerifyResult.PASSED
    }

    /**
     * 执行消耗（校验通过后调用）。
     *
     * @param conditions 条件引用：key → override props
     * @param player     Bukkit Player
     * @param contextVars 额外上下文变量（供 props 公式引用）
     */
    fun consume(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        contextVars: Map<String, Any> = emptyMap()
    ) {
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null

        val session = ScriptManager.openReusableSession(ScriptOptions.of(), emptySet())
        try {
            for ((key, overrideProps) in conditions) {
                val cfg = Planners.conditions.get()[key]
                if (cfg == null) {
                    error("Unknown condition key: $key")
                }
                if (cfg.consume == null) {
                    continue
                }
                cfg.registerSources()
                val resolvedProps = resolveProps(cfg.props, overrideProps, player, profile, router, route, contextVars)
                val props = resolvedProps.values
                val options = createOptions(player, profile, router, route)
                options.set("props", props)
                for ((contextKey, contextValue) in contextVars) {
                    options.set(contextKey, contextValue)
                }
                ScriptManager.rebindReusableSession(session, options, setOf("props"))
                cfg.consume(session, props)
            }
        } finally {
            session.close()
        }
    }

    // ---- 内部 ----

    /**
     * 合并默认 props 与调用处覆盖值，并将 String 值通过预编译函数计算为实际值。
     */
    private fun resolveProps(
        defaultProps: Map<String, Any>,
        overrideProps: Map<String, Any>,
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        contextVars: Map<String, Any>,
        profiling: BatchProfiling? = null
    ): ResolvedProps {
        val merged = defaultProps.toMutableMap()
        merged.putAll(overrideProps)

        val resolved = LinkedHashMap<String, Any>()
        for ((k, v) in merged) {
            resolved[k] = when (v) {
                is String -> {
                    evalValue(v, player, profile, router, route, contextVars, profiling)
                }
                else -> v
            }
        }
        return ResolvedProps(resolved)
    }

    /**
     * 对 String 值尝试求值：
     * 1. 纯数字 → 转为 Int/Double
     * 2. Nova 公式 → 调用预编译函数并返回结果
     */
    private fun evalValue(
        expr: String,
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        contextVars: Map<String, Any>,
        profiling: BatchProfiling?
    ): Any {
        // 纯数字字符串
        val doubleValue = expr.toDoubleOrNull()
        if (doubleValue != null) {
            if (doubleValue % 1 == 0.0) {
                return doubleValue.toInt()
            } else {
                return doubleValue
            }
        }
        val options = createOptions(player, profile, router, route)
        for ((key, value) in contextVars) {
            options.set(key, value)
        }
        val expressionStart = System.nanoTime()
        val result = invokePersistentExpression(expr, options, contextVars, profiling)
        if (profiling != null) {
            profiling.propertyExpressionNanos += System.nanoTime() - expressionStart
        }
        if (result == null) {
            error("Condition property expression returned null: $expr")
        }
        return result
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

    private fun createOptions(
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?
    ): ScriptOptions {
        val options = ScriptOptions.of()
        options.set("player", player)
        options.set("profile", profile)
        if (router != null) {
            options.set("router", router)
        }
        if (route != null) {
            options.set("route", route)
        }
        return options
    }

    private fun invokePersistentExpression(
        source: String,
        options: ScriptOptions,
        contextVars: Map<String, Any>,
        profiling: BatchProfiling?
    ): Any? {
        val function = getPropExpression(source)
        if (profiling != null) {
            val invocation = ScriptManager.invokePersistentProfiled(
                function,
                options,
                options.variables["player"],
                options.variables["profile"],
                options.variables["router"],
                options.variables["route"],
                contextVars["treeId"],
                contextVars["nodeId"],
                contextVars["nodeLevel"]
            )
            profiling.recordPropertyInvocation(invocation)
            return invocation.value
        }
        return ScriptManager.invokePersistent(
            function,
            options,
            options.variables["player"],
            options.variables["profile"],
            options.variables["router"],
            options.variables["route"],
            contextVars["treeId"],
            contextVars["nodeId"],
            contextVars["nodeLevel"]
        )
    }

    /**
     * 执行节点条件表达式。
     *
     * 批量模式下基础上下文已在单个节点开始时绑定，只替换当前条件的 props，
     * 防止同一节点的多条条件反复重建全局脚本绑定。
     */
    private fun evalCondition(config: ConditionConfig, session: NovaSession, props: Map<String, Any>): Boolean {
        return config.evaluate(session, props)
    }

    private fun getPropExpression(source: String): NovaScriptUnit {
        val cached = propExpressions[source]
        if (cached != null) {
            return cached
        }
        val compiled = ScriptManager.compileExpression(
            "condition-prop",
            listOf("player", "profile", "router", "route", "treeId", "nodeId", "nodeLevel"),
            source
        )
        val raced = propExpressions.putIfAbsent(source, compiled)
        if (raced != null) {
            return raced
        }
        return compiled
    }

    /**
     * 收集本批请求实际引用的条件。配置 key 已在加载阶段固定，按首次出现顺序安装，
     * 同一个条件即使被多个节点引用也只编译一次。
     */
    private fun collectConditionConfigs(requests: List<VerifyRequest>): List<ConditionConfig> {
        val allConfigs = Planners.conditions.get()
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
