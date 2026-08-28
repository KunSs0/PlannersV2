package com.gitee.planners.module.script

import java.util.concurrent.CompletableFuture

/** 启动期预编译、运行期只接受显式参数的 Nova 纯函数。 */
open class SingletonScript(
    source: String,
    sourceId: String,
    parameters: List<String> = emptyList()
) {

    val action: String = source
    private val unit: NovaScriptUnit?

    init {
        if (action.isEmpty()) {
            unit = null
        } else {
            unit = ScriptManager.compileExpression(sourceId, parameters, action)
        }
    }

    /** @return 当前表达式是否包含可执行内容。 */
    fun isNotNull(): Boolean {
        return action.isNotEmpty()
    }

    /** 按调用方指定的提交方式执行纯函数。 */
    fun run(async: Boolean, vararg arguments: Any?): CompletableFuture<Any?> {
        if (async) {
            return CompletableFuture.supplyAsync {
                eval(*arguments)
            }
        }
        return CompletableFuture.completedFuture(eval(*arguments))
    }

    /** 同步执行已编译纯函数。 */
    fun eval(vararg arguments: Any?): Any? {
        val compiled = unit
        if (compiled == null) {
            return null
        }
        return ScriptManager.invokePure(compiled, *arguments)
    }
}
