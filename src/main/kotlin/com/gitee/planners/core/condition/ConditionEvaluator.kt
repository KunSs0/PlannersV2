package com.gitee.planners.core.condition

import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.ScriptContext
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.scriptengine.api.ScriptSession
import org.bukkit.entity.Player

/**
 * 条件执行器。
 * 集中定义在 config.yml 的条件模板通过 key 引用 + 传参覆盖后，由本类执行。
 */
class ConditionEvaluator {

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
        return verifyInternal(conditions, player, contextVars, null)
    }

    /**
     * 在同一脚本会话中批量校验多个条件组。
     *
     * 每个请求保持与 [verify] 一致的条件顺序和失败短路语义，
     * 仅复用 GraalJS Context 以避免节点数量线性放大会话创建开销。
     *
     * @param requests 待校验请求列表。
     * @param player 当前玩家。
     * @return 请求 ID 到校验结果的映射。
     */
    fun verifyAll(requests: List<VerifyRequest>, player: Player): Map<String, VerifyResult> {
        val result = LinkedHashMap<String, VerifyResult>()
        if (requests.isEmpty()) {
            return result
        }
        val conditionConfigs = collectConditionConfigs(requests)
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null
        val options = createOptions(player, profile, router, route)
        val previousContext = ScriptContext.getCurrent()
        val session = ScriptManager.openReusableSession(ScriptOptions.of(), emptySet())
        try {
            for (conditionConfig in conditionConfigs) {
                conditionConfig.install(session)
            }
            ScriptContext.setCurrent(options.variables)
            ScriptManager.rebindReusableSession(session, options, setOf("props"))
            for (request in requests) {
                result[request.id] = verifyWithBoundSession(
                    request.conditions,
                    player,
                    profile,
                    router,
                    route,
                    request.contextVars,
                    options,
                    session
                )
            }
        } finally {
            if (previousContext == null) {
                ScriptContext.clear()
            } else {
                ScriptContext.setCurrent(previousContext)
            }
            session.close()
        }
        return result
    }

    private fun verifyInternal(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        contextVars: Map<String, Any>,
        session: ScriptSession?
    ): VerifyResult {
        val profile = player.plannersTemplate
        val router = profile.playerRouter
        val route = if (router != null) router.currentRoute else null
        val hints = mutableListOf<String>()
        val options = createOptions(player, profile, router, route)
        val previousContext = ScriptContext.getCurrent()
        if (session != null) {
            ScriptContext.setCurrent(options.variables)
            ScriptManager.rebindReusableSession(session, options, setOf("props"))
        }

        try {
            for ((key, overrideProps) in conditions) {
                val cfg = Planners.conditions.get()[key]
                if (cfg == null) {
                    error("Unknown condition key: $key")
                }
                val resolvedProps = resolveProps(cfg.props, overrideProps, player, profile, router, route, contextVars, session)
                val props = resolvedProps.values
                options.set("props", props)

                val passed = try {
                    evalCondition(cfg, options, session, props)
                } catch (e: Exception) {
                    false
                }

                if (!passed) {
                    hints.add(interpolate(cfg.hint, props))
                    return VerifyResult(false, hints)
                }
            }
        } finally {
            if (session != null) {
                if (previousContext == null) {
                    ScriptContext.clear()
                } else {
                    ScriptContext.setCurrent(previousContext)
                }
            }
        }
        return VerifyResult.PASSED
    }

    /**
     * 执行已绑定批量会话中的一个节点条件组。
     *
     * player、profile、router、route 在当前职业阶段内不变，批次开始时已经写入会话；
     * 每个节点只更新 props。只有 props 自身包含 JS 公式时，才为了注入其局部上下文重绑一次。
     */
    private fun verifyWithBoundSession(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        contextVars: Map<String, Any>,
        options: ScriptOptions,
        session: ScriptSession
    ): VerifyResult {
        val hints = ArrayList<String>()
        for ((key, overrideProps) in conditions) {
            val config = Planners.conditions.get()[key]
            if (config == null) {
                error("Unknown condition key: $key")
            }
            val resolvedProps = resolveProps(
                config.props,
                overrideProps,
                player,
                profile,
                router,
                route,
                contextVars,
                session
            )
            if (resolvedProps.reboundSession) {
                ScriptManager.rebindReusableSession(session, options, setOf("props"))
            }
            val props = resolvedProps.values
            options.set("props", props)
            val passed = try {
                evalCondition(config, options, session, props)
            } catch (exception: Exception) {
                false
            }
            if (!passed) {
                hints.add(interpolate(config.hint, props))
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

        for ((key, overrideProps) in conditions) {
            val cfg = Planners.conditions.get()[key]
            if (cfg == null) {
                error("Unknown condition key: $key")
            }
            if (cfg.consume == null) {
                continue
            }

            val resolvedProps = resolveProps(cfg.props, overrideProps, player, profile, router, route, contextVars, null)
            val props = resolvedProps.values

            val options = createOptions(player, profile, router, route)
            options.set("props", props)

            try {
                ScriptManager.eval(cfg.consume, options)
            } catch (e: Exception) {
                // consume 失败不阻塞后续
                e.printStackTrace()
            }
        }
    }

    // ---- 内部 ----

    /**
     * 合并默认 props 与调用处覆盖值，并将 String 值 eval 为实际值。
     */
    private fun resolveProps(
        defaultProps: Map<String, Any>,
        overrideProps: Map<String, Any>,
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        contextVars: Map<String, Any>,
        session: ScriptSession?
    ): ResolvedProps {
        val merged = defaultProps.toMutableMap()
        merged.putAll(overrideProps)

        val resolved = LinkedHashMap<String, Any>()
        var reboundSession = false
        for ((k, v) in merged) {
            resolved[k] = when (v) {
                is String -> {
                    if (session != null && v.toDoubleOrNull() == null) {
                        reboundSession = true
                    }
                    evalValue(v, player, profile, router, route, contextVars, session)
                }
                else -> v
            }
        }
        return ResolvedProps(resolved, reboundSession)
    }

    /**
     * 对 String 值尝试求值：
     * 1. 纯数字 → 转为 Int/Double
     * 2. JS 公式 → 执行并返回结果
     * 3. eval 抛异常 → 作为字面量字符串
     */
    private fun evalValue(
        expr: String,
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        contextVars: Map<String, Any>,
        session: ScriptSession?
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
        // JS 公式
        return try {
            val options = createOptions(player, profile, router, route)
            contextVars.forEach { (k, v) -> options.set(k, v) }
            val value = eval(expr, options, session)
            if (value == null) {
                expr
            } else {
                value
            }
        } catch (e: Exception) {
            expr
        }
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

    private fun eval(source: String, options: ScriptOptions, session: ScriptSession?): Any? {
        if (session == null) {
            return ScriptManager.eval(source, options)
        }
        val previousContext = ScriptContext.getCurrent()
        ScriptContext.setCurrent(options.variables)
        try {
            ScriptManager.rebindReusableSession(session, options, emptySet())
            return ScriptManager.eval(session, source)
        } finally {
            if (previousContext == null) {
                ScriptContext.clear()
            } else {
                ScriptContext.setCurrent(previousContext)
            }
        }
    }

    /**
     * 执行节点条件表达式。
     *
     * 批量模式下基础上下文已在单个节点开始时绑定，只替换当前条件的 props，
     * 防止同一节点的多条条件反复重建全局脚本绑定。
     */
    private fun evalCondition(
        config: ConditionConfig,
        options: ScriptOptions,
        session: ScriptSession?,
        props: Map<String, Any>
    ): Boolean {
        if (session == null) {
            return ScriptManager.eval(config.exper, options) == true
        }
        ScriptManager.setReusableSessionBinding(session, "props", props)
        return config.evaluate(session)
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
        val values: Map<String, Any>,
        val reboundSession: Boolean
    )

}
