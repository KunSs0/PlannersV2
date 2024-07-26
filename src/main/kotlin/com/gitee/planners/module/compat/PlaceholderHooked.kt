package com.gitee.planners.module.compat

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.common.script.SingletonKetherScript
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext
import org.bukkit.entity.Player
import taboolib.platform.compat.PlaceholderExpansion

object PlaceholderHooked : PlaceholderExpansion {

    private val platform = ComplexScriptPlatform.DefaultComplexScriptPlatform()

    private val namespaces = listOf(KetherHelper.NAMESPACE_COMMON)

    override val identifier: String
        get() = "planners"

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        val sender = adaptTarget<Target<*>>(player!!)
        val compiledScript = createSimpleComplexScript(identifier, args, namespaces, platform)
        val context = createSimpleContext(sender, compiledScript)

        return context.run(KetherScriptOptions.common(sender)).thenApply { it.toString() }.get()
    }

    fun createSimpleContext(sender: Target<*>,complexScript: ComplexCompiledScript): AbstractComplexScriptContext {

        return object : AbstractComplexScriptContext(sender,complexScript) {

            override val trackId: String
                get() = "PlaceholderAPI"

        }
    }

    fun createSimpleComplexScript(id: String, action: String, namespace: List<String>,platform: ComplexScriptPlatform): ComplexCompiledScript {
        val script = object : ComplexCompiledScript {

            override val id: String = id

            override val async: Boolean = false

            override fun source(): String {
                return action
            }

            override fun namespaces(): List<String> {
                return namespace
            }

            override fun platform(): ComplexScriptPlatform {
                return platform
            }

        }

        return script
    }


}
