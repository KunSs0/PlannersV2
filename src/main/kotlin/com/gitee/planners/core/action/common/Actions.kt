package com.gitee.planners.core.action.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.common.script.SingletonKetherScript
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.core.action.context.AbstractSkillContext
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetCommandSender
import com.gitee.planners.core.action.*
import com.gitee.planners.core.action.context.AbstractComplexScriptContext
import com.gitee.planners.core.action.context.CompiledScriptContext
import org.bukkit.Bukkit
import taboolib.common.util.random
import taboolib.module.kether.*


@KetherEditor.Document(value = "lazy <id>", result = Any::class)
@CombinationKetherParser.Used
private fun actionLazyVar() = KetherHelper.combinedKetherParser("lazy") {
    it.group(text()).apply(it) { id ->
        future {
            getEnvironmentContext().castUnsafely<AbstractSkillContext>()?.variables?.get(id)?.value ?: error("Unable to get variable $id")
        }
    }
}

@KetherEditor.Document(value = "tell <message> <at <objective...>>", result = Void::class)
@CombinationKetherParser.Used
private fun actionTell() = KetherHelper.combinedKetherParser("tell") {
    it.group(text(), commandObjective(type = LeastType.SENDER)).apply(it) { message, container ->
        now {
            container.filterIsInstance<TargetCommandSender<*>>().forEach {
                it.sendMessage(message)
            }
        }
    }
}

@KetherEditor.Document(value = "chance <value: double>", result = Boolean::class)
@CombinationKetherParser.Used
private fun actionChance() = KetherHelper.combinedKetherParser("chance") {
    it.group(actionDouble()).apply(it) { value ->
        now { random(value) }
    }
}

/**
 * 内联函数
 */
@KetherEditor.Document(value = "inline <text>", result = String::class)
@CombinationKetherParser.Used
private fun actionFunction() = KetherHelper.combinedKetherParser("inline", "function") {
    it.group(text()).apply(it) { text ->
        now {
            val context = this.getEnvironmentContext()
            val options = if (context is AbstractComplexScriptContext) {
                context.createOptions {
                    vars(deepVars())
                }
            } else {
                KetherScriptOptions.create {
                    namespace(listOf(KetherHelper.NAMESPACE_COMMON))
                    vars(deepVars())
                    sender(this@now.script().sender ?: return@create)
                }
            }
            runKether(text) {
                SingletonKetherScript(text).run(options).getNow(null).toString()
            }
        }
    }
}
