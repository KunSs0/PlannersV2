package com.gitee.planners.module.compat.placeholder

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext
import org.bukkit.entity.Player

/**
 * PlaceholderScript 占位符脚本
 */
object PlaceholderScript {

    private val namespaces = listOf(KetherHelper.NAMESPACE_COMMON)

    private val platform = ComplexScriptPlatform.DefaultComplexScriptPlatform()

    fun parse(player: Player, args: String): String {
        val sender = adaptTarget<Target<*>>(player)
        val compiledScript = createSimpleComplexScript(PlaceholderHooked.identifier, args, namespaces, platform)
        val context = createSimpleContext(sender, compiledScript)

        return context.run(KetherScriptOptions.common(sender)).thenApply { it.toString() }.get()
    }

    fun createSimpleContext(sender: Target<*>, complexScript: ComplexCompiledScript): AbstractComplexScriptContext {

        return object : AbstractComplexScriptContext(sender, complexScript) {

            override val trackId: String
                get() = "PlaceholderAPI"

        }
    }

    fun createSimpleComplexScript(
        id: String,
        action: String,
        namespace: List<String>,
        platform: ComplexScriptPlatform
    ): ComplexCompiledScript {
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
