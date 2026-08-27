package com.gitee.planners.module.script

import java.util.concurrent.CompletableFuture

/**
 * 单个 Nova 表达式的启动期预编译封装。
 *
 * @param source Nova 表达式源码。
 * @param sourceId YAML 业务节点的稳定标识。
 */
open class SingletonScript(source: String, sourceId: String) : Script {

    val action: String = source
    private val unit: NovaScriptUnit?

    init {
        if (action.isEmpty()) {
            unit = null
        } else {
            unit = ScriptManager.compileExpression(sourceId, action)
        }
    }

    /** @return 当前表达式是否包含可执行内容。 */
    fun isNotNull(): Boolean {
        return action.isNotEmpty()
    }

    /**
     * 执行预编译表达式。
     *
     * `async` 只决定 Future 的提交方式；真正的 Bukkit 业务仍由 Workspace 的
     * MAIN_THREAD 策略显式调度并同步等待。
     */
    override fun run(options: ScriptOptions): CompletableFuture<Any?> {
        val compiled = unit
        if (compiled == null) {
            return CompletableFuture.completedFuture(null)
        }
        if (options.isAsync) {
            return CompletableFuture.supplyAsync {
                ScriptManager.invokeCompiled(compiled, options)
            }
        }
        return CompletableFuture.completedFuture(ScriptManager.invokeCompiled(compiled, options))
    }

    /** @return 当前表达式的同步结果。 */
    fun eval(options: ScriptOptions): Any? {
        val compiled = unit
        if (compiled == null) {
            return null
        }
        return ScriptManager.invokeCompiled(compiled, options)
    }

    /** @return 无业务绑定时的同步结果。 */
    fun eval(): Any? {
        return eval(ScriptOptions.of())
    }

    /**
     * 在现有会话中执行表达式。
     *
     * @param session 当前资源会话。
     * @return 表达式结果。
     */
    fun invoke(session: NovaSession): Any? {
        val compiled = unit
        if (compiled == null) {
            return null
        }
        return session.invoke(compiled)
    }

}
