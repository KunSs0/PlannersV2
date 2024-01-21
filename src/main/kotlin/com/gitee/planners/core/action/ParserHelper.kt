package com.gitee.planners.core.action

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.core.action.selector.ActionTargetContainer
import org.bukkit.Material
import org.bukkit.util.Vector
import taboolib.common.platform.command.command
import taboolib.common5.cbool
import taboolib.common5.cdouble
import taboolib.common5.cfloat
import taboolib.common5.cint
import taboolib.library.kether.LoadError
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.Parser
import taboolib.library.kether.Parser.Action
import taboolib.library.kether.QuestReader
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.module.kether.ParserHolder
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.action.ActionLiteral
import taboolib.module.kether.literalAction
import java.util.concurrent.CompletableFuture

/**
 * 捕获材质
 */
fun ParserHolder.mat(defaultValue: Material = Material.STONE): Parser<Material> {
    return any().map {
        Material.valueOf(it?.toString()?.uppercase() ?: return@map null)
    }.defaultsTo(defaultValue)
}

inline fun <reified T> ParserHolder.actionType(crossinline func: (Any?) -> T): Parser<T> {
    return any().map { func(it) }
}

fun ParserHolder.actionText() = actionType { it.toString() }

fun ParserHolder.actionInt() = actionType { it.cint }

fun ParserHolder.actionDouble() = actionType { it.cdouble }

fun ParserHolder.actionFloat() = actionType { it.cfloat }

fun ParserHolder.actionBool() = actionType { it.cbool }

fun ParserHolder.actionVector() = actionDouble().and(actionDouble(), actionDouble()).map {
    val (x, y, z) = it
    Vector(x, y, z)
}

fun QuestReader.expectParsedAction(token: String, defaultValue: Any?): ParsedAction<*> {
    val parsedAction = this.expectParsedActionOrNull(token)
    if (parsedAction == null && (defaultValue !is ActionLiteral<*> && defaultValue == null)) {
        throw LoadError.UNKNOWN_ACTION.create(*arrayOf<Any>(this.nextToken()));
    }
    return parsedAction ?: if (defaultValue is ActionLiteral<*>) {
        ParsedAction(defaultValue)
    } else {
        literalAction(defaultValue!!)
    }
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

fun ScriptFrame.runTargetContainer(action: ActionTargetContainer): CompletableFuture<TargetContainer> {
    return action.process(this)
}

fun QuestReader.expectTargetContainerParsedAction(type: LeastType): ActionTargetContainer {
    return ActionTargetContainer.parser(this, type)
}

fun ParserHolder.objective(type: LeastType = LeastType.EMPTY): Parser<TargetContainer> {
    return Parser.frame { r ->
        val parser = ActionTargetContainer.parser(r, type, true)
        Action { frame ->
            parser.process(frame)
        }
    }
}

fun ParserHolder.commandObjective(type: LeastType = LeastType.EMPTY): Parser<TargetContainer> {
    return Parser.frame { r ->
        val parser = ActionTargetContainer.parser(r, type, false)
        Action { frame ->
            parser.process(frame)
        }
    }
}

inline fun <reified T : Enum<T>> getEnumWithIdOrNull(name: String): T? {
    return T::class.java.enumConstants.firstOrNull { it.name == name }
}

inline fun <reified T : Enum<T>> getEnumWithId(name: String): T {
    return getEnumWithIdOrNull<T>(name) ?: error("Enum $name not found it")
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Enum<T>> ParserHolder.enum(): Parser<T> {
    return enumOrNull<T>() as Parser<T>
}

inline fun <reified T : Enum<T>> ParserHolder.enumOrNull(): Parser<T?> {
    return Parser.of { it.nextToken() }.orElse(any().map { it!!.toString() }).map { getEnumWithId<T>(it.uppercase()) }
}

inline fun <reified T : Enum<T>> ParserHolder.enumListOf(): Parser<List<T>> {
    return tokenListOf { getEnumWithId<T>(it.uppercase()) }
}

inline fun <reified T : Enum<T>> ParserHolder.commandEnumOrNull(name: String): Parser<T?> {
    return command(name, then = enumOrNull<T>()).option()
}

inline fun <reified T : Enum<T>> ParserHolder.commandEnum(name: String, defaultValue: T): Parser<T> {
    return command(name, then = enumOrNull<T>()).option().defaultsTo(defaultValue)
}

// base

inline fun <reified T : Enum<T>> ParserHolder.commandEnumListOf(
    name: String,
    fill: List<T> = emptyList()
): Parser<List<T>> {
    return command(name, then = enumListOf<T>()).option().defaultsTo(fill)
}

fun ParserHolder.commandText(name: String, defaultValue: String = ""): Parser<String> {
    return command(name, then = text()).option().defaultsTo(defaultValue)
}

fun ParserHolder.commandInt(name: String, defaultValue: Int = 0): Parser<Int> {
    return command(name, then = int()).option().defaultsTo(defaultValue)
}

fun ParserHolder.commandBool(token: String, defaultValue: Boolean = false): Parser<Boolean> {
    return command(token, then = bool()).option().defaultsTo(defaultValue)
}

fun ParserHolder.commandFloat(token: String, defaultValue: Float = 0f): Parser<Float> {
    return command(token, then = float()).option().defaultsTo(defaultValue)
}

fun ParserHolder.commandDouble(token: String, defaultValue: Double = 0.0): Parser<Double> {
    return command(token, then = double()).option().defaultsTo(defaultValue)
}

/**
 * 模糊匹配 [] -> [] , "" -> []
 */
inline fun <reified T> ParserHolder.tokenListOf(crossinline block: (String) -> T): Parser<List<T>> {
    return Parser.frame { r ->
        val list = ArrayList<String>()
        try {
            r.mark()
            r.expect("[")
            while (r.hasNext() && r.peek() != ']') {
                list.add(r.nextToken().uppercase())
            }
            r.expect("]")
        } catch (e: Exception) {
            r.reset()
            list.add(r.nextToken().uppercase())
        }
        list.trimToSize()
        now {
            list.map { token -> block(token) }
        }
    }
}
