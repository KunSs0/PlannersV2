package com.gitee.planners.module.kether.context

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.library.kether.Quest
import taboolib.module.kether.ScriptOptions
import java.util.concurrent.CompletableFuture

abstract class AbstractComplexScriptContext(sender: Target<*>, val compiled: ComplexCompiledScript) :
    AbstractContext(sender) {

    abstract val trackId: String

    val platform = compiled.platform()

    /** 是否异步运行 */
    var async = compiled.async

    /** 是否立即执行 如果为true则忽略async，同步当前线程执行 */
    var now = false

    override fun call(): CompletableFuture<Any> {

        val future = CompletableFuture<Any>()
        submit(async = async) {
            platform.run(trackId, compiled.compiledScript(), this@AbstractComplexScriptContext.createOptions())
                .thenAccept {
                    future.complete(it)
                }
        }
        return future
    }

    open fun call(block: Quest.Block, func: ScriptOptions.ScriptOptionsBuilder.() -> Unit = { }) {
        submit(async = async) {
            platform.run(
                trackId,
                compiled.compiledScript(),
                block,
                this@AbstractComplexScriptContext.createOptions(func)
            )
        }
    }

    open fun createOptions(block: ScriptOptions.ScriptOptionsBuilder.() -> Unit = {}): KetherScriptOptions {
        return KetherScriptOptions.create {
            namespace(compiled.namespaces())
            // set sender
            if (this@AbstractComplexScriptContext.sender is TargetBukkitEntity && this@AbstractComplexScriptContext.sender.instance is Player) {
                this.sender(this@AbstractComplexScriptContext.sender.instance as Player)
            }
            // 注入变量
            this.vars("@running-environment-context" to this@AbstractComplexScriptContext)
            block(this)
        }
    }
}
