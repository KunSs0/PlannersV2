package com.gitee.planners.core.condition

import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.scriptengine.api.ScriptSession

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
        "var props = __plannersConditionProps; return ($exper);"
    )

    private val batchFunction = ScriptManager.compileAction(
        "condition:$key:batch",
        listOf("player", "profile", "router", "route", "values"),
        "var result = '';" +
            "for (var i = 0; i < values.length; i++) {" +
            "var input = values[i];" +
            "var props = input.props;" +
            "var treeId = input.treeId;" +
            "var nodeId = input.nodeId;" +
            "var nodeLevel = input.nodeLevel;" +
            "result += ($exper) ? '1' : '0';" +
            "}" +
            "return result;"
    )

    private val consumeFunction = if (consume == null) {
        null
    } else {
        ScriptManager.compileAction(
            "condition:$key:consume",
            "var props = __plannersConditionProps;" + consume
        )
    }

    private var persistentInstalled = false

    /**
     * 将条件批量函数安装到长期 ScriptEngine 会话。
     *
     * 配置节点会在 Bukkit 插件启用前解码，因此不能在构造阶段创建 Graal Context。
     * 插件启用时主动调用本方法；配置热重载产生的新条件由首次批量校验兜底安装。
     */
    @Synchronized
    fun installPersistent() {
        if (persistentInstalled) {
            return
        }
        ScriptManager.installPersistent(batchFunction)
        persistentInstalled = true
    }

    /** 将条件函数安装到当前会话。 */
    fun install(session: ScriptSession) {
        ScriptManager.installCompiledFunction(session, expressionFunction)
        val consumeFunction = this.consumeFunction
        if (consumeFunction != null) {
            ScriptManager.installCompiledFunction(session, consumeFunction)
        }
    }

    /** 使用当前会话已绑定的 player、route 等上下文执行条件。 */
    fun evaluate(session: ScriptSession, conditionProps: Map<String, Any>): Boolean {
        ScriptManager.setReusableSessionBinding(session, "__plannersConditionProps", conditionProps)
        return ScriptManager.invokeCompiled(session, expressionFunction) == true
    }

    /**
     * 在长期工作区 Context 内批量执行条件。
     *
     * 参数均为函数局部变量，不写入 JavaScript 全局作用域。
     */
    fun evaluateBatchPersistent(
        options: ScriptOptions,
        conditionInputs: List<Map<String, Any>>
    ): BatchEvaluation {
        installPersistent()
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
    fun consume(session: ScriptSession, conditionProps: Map<String, Any>) {
        val consumeFunction = this.consumeFunction
        if (consumeFunction == null) {
            return
        }
        ScriptManager.setReusableSessionBinding(session, "__plannersConditionProps", conditionProps)
        ScriptManager.invokeCompiled(session, consumeFunction)
    }
}
