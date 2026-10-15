package com.gitee.planners.core.condition

import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 不可变条件定义。
 *
 * 条件文本在配置加载时包装为命名函数；Workspace 启动时统一预编译，运行期只调用函数，
 * 禁止将条件表达式作为字符串再次执行。
 */
class NovaConditionConfig(
    key: String,
    val exper: String,
    props: Map<String, Any>,
    hint: String,
    consume: String?
) : ConditionConfig(key, props, hint, consume), ConditionExecution {

    override val expressionProperties: Boolean = true
    override val preparationKey: String = "nova:$key"

    /** Nova 条件无需读取外部数据，直接发布当前执行器。 */
    override fun prepare(playerId: UUID): CompletableFuture<ConditionExecution> {
        return CompletableFuture.completedFuture(this)
    }

    private val expressionFunction = ScriptManager.compileAction(
        "condition:$key:expression",
        listOf("player", "profile", "router", "route", "props"),
        "return ($exper)"
    )

    private val batchFunction = ScriptManager.compileAction(
        "condition:$key:batch",
        listOf("player", "profile", "router", "route", "values"),
        "var result = \"\"\n" +
            "for (input in values) {\n" +
            "val props = input.props\n" +
            "val treeId = input.treeId\n" +
            "val nodeId = input.nodeId\n" +
            "val nodeLevel = input.nodeLevel\n" +
            "result += if ($exper) \"1\" else \"0\"\n" +
            "}\n" +
            "return result"
    )

    private val consumeFunction = if (consume == null) {
        null
    } else {
        ScriptManager.compileAction(
            "condition:$key:consume",
            listOf("player", "profile", "router", "route", "props"),
            consume
        )
    }

    /**
     * 校验条件入口已经在当前 Workspace 加载前完成登记。
     *
     * 条件对象构造函数已经登记表达式、批处理与消耗 SourceUnit；这里显式验证模块标识，
     * 供插件启动和重建流程清楚表达“收集条件源码”阶段。
     */
    override fun registerSources() {
        if (expressionFunction.moduleId.isBlank() || batchFunction.moduleId.isBlank()) {
            throw IllegalStateException("A condition Nova module ID must not be blank: $key")
        }
        val consumeFunction = this.consumeFunction
        if (consumeFunction != null && consumeFunction.moduleId.isBlank()) {
            throw IllegalStateException("A condition consume Nova module ID must not be blank: $key")
        }
    }

    /** 使用完整显式业务参数执行已编译条件纯函数。 */
    override fun evaluate(
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        conditionProps: Map<String, Any>
    ): Boolean {
        return ScriptManager.invokePure(expressionFunction, player, profile, router, route, conditionProps) == true
    }

    /**
     * 在 Workspace 持久资源作用域内批量执行条件。
     *
     * 参数均为函数局部变量，不写入 Nova 全局作用域。
     */
    override fun evaluateBatchPersistent(
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        conditionInputs: List<Map<String, Any>>
    ): ConditionExecution.BatchEvaluation {
        val invocation = ScriptManager.invokePureProfiled(
            batchFunction,
            player,
            profile,
            router,
            route,
            conditionInputs
        )
        val result = invocation.value
        if (result == null) {
            return ConditionExecution.BatchEvaluation("", invocation)
        }
        return ConditionExecution.BatchEvaluation(result.toString(), invocation)
    }

    /** 执行已经验证通过的条件消耗。 */
    override fun consume(
        player: Player,
        profile: PlayerTemplate,
        router: PlayerRouter?,
        route: PlayerRoute?,
        conditionProps: Map<String, Any>
    ) {
        val consumeFunction = this.consumeFunction
        if (consumeFunction == null) {
            return
        }
        ScriptManager.invokePure(consumeFunction, player, profile, router, route, conditionProps)
    }
}
