package com.gitee.planners.core.action

import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.core.action.selector.ActionTargetContainer
import com.gitee.planners.core.action.selector.InVariable
import org.bukkit.Material
import taboolib.library.kether.*
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

/**
 * 捕获材质
 */
fun ParserHolder.mat(defaultValue: Material = Material.STONE): Parser<Material> {
    return any().map {
        Material.valueOf(it?.toString()?.uppercase() ?: return@map null)
    }.defaultsTo(defaultValue)
}

inline fun <reified T : Enum<T>> ParserHolder.enum(defaultValue: T): Parser<T> {
    return text().map { s ->
        T::class.java.enumConstants.firstOrNull { it.name == s } ?: defaultValue
    }
}

fun QuestReader.expectParsedAction(token: String, defaultValue: Any?): ParsedAction<*> {
    val parsedAction = this.expectParsedActionOrNull(token)
    if (parsedAction == null && defaultValue == null) {
        throw LoadError.UNKNOWN_ACTION.create(*arrayOf<Any>(this.nextToken()));
    }
    return parsedAction ?: literalAction(defaultValue!!)
}

fun QuestReader.expectParsedActionOrNull(token: String): ParsedAction<*>? {
    return try {
        this.mark()
        this.expect(token)
        this.nextParsedAction()
    } catch (e: Exception) {
        this.reset()
        null
    }
}

fun ScriptFrame.runTargetContainer(action: ActionTargetContainer) : CompletableFuture<TargetContainer> {
    return action.process(this)
}

fun QuestReader.expectTargetContainerParsedAction(type: LeastType): ActionTargetContainer {
    return ActionTargetContainer.parser(this,type)
}

fun ParserHolder.objective(type: LeastType = LeastType.EMPTY): Parser<TargetContainer> {
    return Parser.frame { reader ->
        val action = reader.expectTargetContainerParsedAction(type)
        future { action.process(this) }
    }
}
