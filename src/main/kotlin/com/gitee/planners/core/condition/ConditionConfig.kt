package com.gitee.planners.core.condition

import com.gitee.planners.module.script.ScriptManager
import com.gitee.scriptengine.api.ScriptSession

/**
 * 不可变条件定义。
 *
 * 条件表达式在批量校验会话中安装成固定函数。这样节点校验只需重绑动态上下文和调用函数，
 * 不会为每个节点重复解析同一段 JavaScript 文本。
 */
class ConditionConfig(
    val key: String,
    val exper: String,
    val props: Map<String, Any>,
    val hint: String,
    val consume: String?
) {

    private val functionName = createFunctionName(key)
    private val batchFunctionName = functionName + "_batch"

    /**
     * 将此条件表达式装载为当前会话内的固定函数。
     */
    fun install(session: ScriptSession) {
        val source = "function $functionName(__plannersProps) { var props = __plannersProps; return ($exper); }" +
            " function $batchFunctionName(__plannersPropsList) {" +
            " var result = '';" +
            " for (var i = 0; i < __plannersPropsList.size(); i++) {" +
            " result += $functionName(__plannersPropsList.get(i)) ? '1' : '0';" +
            " }" +
            " return result;" +
            " }"
        ScriptManager.eval(session, source)
    }

    /**
     * 使用当前会话已绑定的 player、route、props 等动态上下文执行条件。
     */
    fun evaluate(session: ScriptSession, conditionProps: Map<String, Any>): Boolean {
        return ScriptManager.invoke(session, functionName, conditionProps) == true
    }

    fun getFunctionName(): String {
        return functionName
    }

    fun evaluateBatch(session: ScriptSession, conditionProps: List<Map<String, Any>>): String {
        val result = ScriptManager.invoke(session, batchFunctionName, conditionProps)
        if (result == null) {
            return ""
        }
        return result.toString()
    }

    private fun createFunctionName(conditionKey: String): String {
        val result = StringBuilder("__plannersCondition_")
        val bytes = conditionKey.toByteArray(Charsets.UTF_8)
        for (byte in bytes) {
            val hex = (byte.toInt() and 0xFF).toString(16)
            if (hex.length == 1) {
                result.append('0')
            }
            result.append(hex)
        }
        return result.toString()
    }
}
