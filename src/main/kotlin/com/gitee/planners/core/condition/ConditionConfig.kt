package com.gitee.planners.core.condition

import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.NovaSession

/**
 * 不可变条件定义。
 *
 * 条件文本在配置加载时包装为命名函数；每个会话只安装一次，运行期只调用函数，
 * 禁止将条件表达式作为字符串再次执行。
 */
class ConditionConfig(
    val key: String,
    val exper: String,
    val props: Map<String, Any>,
    val hint: String,
    val consume: String?
) {

    private val expressionFunction = ScriptManager.compileAction(
        "condition:$key:expression",
        "val props = __plannersConditionProps\nreturn ($exper)"
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
            "val props = __plannersConditionProps\n" + consume
        )
    }

    /**
     * 校验条件入口已经在当前 Workspace 加载前完成登记。
     *
     * 条件对象构造函数已经登记表达式、批处理与消耗 SourceUnit；这里显式验证模块标识，
     * 供插件启动和重建流程清楚表达“收集条件源码”阶段。
     */
    fun registerSources() {
        if (expressionFunction.moduleId.isBlank() || batchFunction.moduleId.isBlank()) {
            throw IllegalStateException("A condition Nova module ID must not be blank: $key")
        }
        val consumeFunction = this.consumeFunction
        if (consumeFunction != null && consumeFunction.moduleId.isBlank()) {
            throw IllegalStateException("A condition consume Nova module ID must not be blank: $key")
        }
    }

    /** 使用当前会话已绑定的 player、route 等上下文执行条件。 */
    fun evaluate(session: NovaSession, conditionProps: Map<String, Any>): Boolean {
        ScriptManager.setReusableSessionBinding(session, "__plannersConditionProps", conditionProps)
        return ScriptManager.invokeCompiled(session, expressionFunction) == true
    }

    /**
     * 在长期工作区 Context 内批量执行条件。
     *
     * 参数均为函数局部变量，不写入 Nova 全局作用域。
     */
    fun evaluateBatchPersistent(
        options: ScriptOptions,
        conditionInputs: List<Map<String, Any>>
    ): BatchEvaluation {
        val invocation = ScriptManager.invokePersistentProfiled(
            batchFunction,
            options,
            options.variables["player"],
            options.variables["profile"],
            options.variables["router"],
            options.variables["route"],
            conditionInputs
        )
        val result = invocation.value
        if (result == null) {
            return BatchEvaluation("", invocation)
        }
        return BatchEvaluation(result.toString(), invocation)
    }

    /**
     * 条件批处理执行结果。
     *
     * @property encoded 每个输入对应一个字符的通过结果。
     * @property invocation 脚本长期会话的分段计时。
     */
    class BatchEvaluation(
        val encoded: String,
        val invocation: ScriptManager.PersistentInvocation
    ) {
    }

    /** 执行已经验证通过的条件消耗。 */
    fun consume(session: NovaSession, conditionProps: Map<String, Any>) {
        val consumeFunction = this.consumeFunction
        if (consumeFunction == null) {
            return
        }
        ScriptManager.setReusableSessionBinding(session, "__plannersConditionProps", conditionProps)
        ScriptManager.invokeCompiled(session, consumeFunction)
    }
}
