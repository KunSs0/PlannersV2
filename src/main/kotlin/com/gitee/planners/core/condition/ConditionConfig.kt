package com.gitee.planners.core.condition

import com.gitee.planners.module.script.ScriptManager
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
        "var values = __plannersConditionProps;" +
            "var result = '';" +
            "for (var i = 0; i < values.size(); i++) {" +
            "var props = values.get(i);" +
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

    /** 将条件函数安装到当前会话。 */
    fun install(session: ScriptSession) {
        ScriptManager.installCompiledFunction(session, expressionFunction)
        ScriptManager.installCompiledFunction(session, batchFunction)
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

    /** 在同一上下文中批量执行同一条件。 */
    fun evaluateBatch(session: ScriptSession, conditionProps: List<Map<String, Any>>): String {
        ScriptManager.setReusableSessionBinding(session, "__plannersConditionProps", conditionProps)
        val result = ScriptManager.invokeCompiled(session, batchFunction)
        if (result == null) {
            return ""
        }
        return result.toString()
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
