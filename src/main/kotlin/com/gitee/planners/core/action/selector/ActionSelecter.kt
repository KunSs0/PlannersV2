package com.gitee.planners.core.action.selector

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.action.getEnvironmentContext
import com.gitee.planners.core.action.getTargetContainer
import org.bukkit.Bukkit
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture


private object Sender : Selector {
    override fun namespace(): Array<String> {
        return arrayOf("sender", "self")
    }

    override fun action(): QuestActionParser {
        return scriptParser {
            actionNow {
                getTargetContainer() += getEnvironmentContext().sender
                null
            }
        }
    }
}

private object Console : Selector {
    override fun namespace(): Array<String> {
        return arrayOf("console")
    }

    override fun action(): QuestActionParser {
        return scriptParser {
            actionNow {
                getTargetContainer() += Bukkit.getConsoleSender().adaptTarget()
                null
            }
        }
    }

}

@KetherEditor.Document(value = "select <objective...>", result = TargetContainer::class)
@CombinationKetherParser.Used
private fun actionSelect() = KetherHelper.simpleKetherParser("select") {

    fun process(frame: ScriptFrame, cur: Int, array: List<QuestAction<*>>): CompletableFuture<Void> {
        return if (cur < array.size) {
            array[cur].process(frame).thenCompose {
                process(frame, cur + 1, array)
            }
        } else {
            CompletableFuture.completedFuture(null)
        }
    }

    scriptParser { reader ->
        val actions = mutableListOf<QuestAction<*>>()
        try {
            reader.mark()
            val expect = reader.expects(*SelectorRegistry.getKeys().map { "@$it" }.toTypedArray())
            val selector = SelectorRegistry.get(expect.substring(1))
            actions += selector.action().resolve<Any>(reader)

        } catch (e: Exception) {
            reader.reset()
        }

        actionNow {
            TargetContainer().also {
                // temp variable
                this.variables()["@RUNNING_TEMP_CONTAINER"] = it
                process(this, 0, actions)
                this.variables().remove("@RUNNING_TEMP_CONTAINER")
            }
        }
    }
}
