package com.gitee.planners.module.script

import com.novalang.workspace.ResourceScope
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 一次 Planners 业务脚本执行所持有的 Nova 绑定与资源作用域。
 *
 * Session 不承载解释器，也不允许运行期编译源码。它只负责复用启动期已预编译入口，
 * 并让 WorkspaceTasks 等脚本资源明确归属于当前业务实例。
 */
class NovaSession internal constructor(
    internal val bindings: LinkedHashMap<String, Any?>,
    internal val scope: ResourceScope
) : AutoCloseable {

    private val closed = AtomicBoolean(false)

    /**
     * 调用预编译模块的指定导出函数。
     *
     * @param unit 已登记的 Nova 模块。
     * @param functionName 导出函数名。
     * @param args 函数参数。
     * @return Nova 返回值。
     */
    fun invoke(unit: NovaScriptUnit, functionName: String = unit.functionName, vararg args: Any?): Any? {
        ensureOpen()
        return ScriptManager.invokeInSession(this, unit, functionName, *args)
    }

    /**
     * 替换当前会话的动态业务绑定。
     *
     * @param key 绑定名称。
     * @param value 绑定值。
     */
    fun bind(key: String, value: Any?) {
        ensureOpen()
        bindings[key] = value
    }

    /**
     * 释放没有持久资源的会话作用域。
     *
     * 创建过 WorkspaceTasks 的作用域由 Workspace 在插件卸载时统一销毁，确保异步回调不会因
     * 主函数返回而被提前取消。
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        if (scope.resourceCount == 0) {
            scope.dispose()
        }
    }

    /** 校验会话仍可接受调用。 */
    private fun ensureOpen() {
        if (closed.get()) {
            throw IllegalStateException("The Nova session has already been closed")
        }
    }
}
