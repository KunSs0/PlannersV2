package com.gitee.planners.core.action.context

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import org.bukkit.entity.Player
import taboolib.module.kether.ScriptOptions

abstract class AbstractComplexScriptContext(sender: Target<*>, val compiled: ComplexCompiledScript) : AbstractContext(sender) {

    abstract val trackId: String

    val platform = compiled.platform()

    override fun process() {
        platform.run(trackId, compiled.compiledScript(), this.createOptions())
    }

    open fun createOptions(block: ScriptOptions.ScriptOptionsBuilder.() -> Unit = {}): KetherScriptOptions {
        return KetherScriptOptions.create {
            namespace(compiled.namespaces())
            // set sender
            if (this@AbstractComplexScriptContext.sender is TargetBukkitEntity && this@AbstractComplexScriptContext.sender.getInstance() is Player) {
                this.sender(this@AbstractComplexScriptContext.sender.getInstance() as Player)
            }
            // 注入变量
            this.vars("@running-environment-context" to this@AbstractComplexScriptContext)
            block(this)
        }
    }
}
