package com.gitee.planners.module.kether

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.module.kether.selector.TargetContainerParser
import com.gitee.planners.module.particle.BukkitParticle
import com.gitee.planners.util.math.asTransformMatrix
import com.gitee.planners.util.math.asVector
import taboolib.common.util.Vector
import taboolib.common5.cbool
import taboolib.common5.cdouble
import taboolib.common5.cfloat
import taboolib.common5.cint
import taboolib.library.kether.*
import taboolib.library.kether.Parser.*
import taboolib.module.kether.*
import taboolib.module.kether.action.ActionLiteral
import java.util.concurrent.CompletableFuture


inline fun <reified T> ParserHolder.actionType(crossinline func: (Any?) -> T): Parser<T> {
    return any().map { func(it) }
}

fun ParserHolder.actionText() = actionType { it.toString() }

fun ParserHolder.actionInt() = actionType { it.cint }

fun ParserHolder.actionDouble() = actionType { it.cdouble }

fun ParserHolder.actionFloat() = actionType { it.cfloat }

fun ParserHolder.actionBool() = actionType { it.cbool }

fun ParserHolder.actionVector() = actionType { it?.asVector() ?: error("Missing argument") }

fun ParserHolder.actionTransformMatrix() = actionType { it?.asTransformMatrix() ?: error("Missing argument") }

fun ParserHolder.actionParticle() = actionType {
    it?.let {
        it as? BukkitParticle ?: error("Invalid particle")
    } ?: error("Missing argument")
}

fun QuestReader.catchParsedAction(token: String, defaultValue: Any?): ParsedAction<*> {
    val parsedAction = this.catchParsedActionOrNull(token)
    if (parsedAction == null && (defaultValue !is ActionLiteral<*> && defaultValue == null)) {
        throw LoadError.UNKNOWN_ACTION.create(*arrayOf<Any>(this.nextToken()));
    }
    return parsedAction ?: if (defaultValue is ActionLiteral<*>) {
        ParsedAction(defaultValue)
    } else {
        literalAction(defaultValue!!)
    }
}

fun QuestReader.catchParsedActionOrNull(token: String): ParsedAction<*>? {
    return try {
        this.mark()
        this.expect(token)
        this.nextParsedAction()
    } catch (e: Exception) {
        this.reset()
        null
    }
}

fun ScriptFrame.runTargetContainer(action: TargetContainerParser): CompletableFuture<TargetContainer> {
    return action.process(this)
}

fun QuestReader.expectTargetContainerParsedAction(type: LeastType): TargetContainerParser {
    return TargetContainerParser.parser(reader = this, type = type)
}

fun ParserHolder.objective(): Parser<TargetContainer> {
    return Parser.frame { r ->
        val parser = TargetContainerParser.parser(emptyArray(), r, LeastType.EMPTY)
        Action {
            parser.process(it)
        }
    }
}

fun ParserHolder.commandObjective(expect: Array<String> = TargetContainerParser.DEFAULT_PREFIX,
                                  type: LeastType = LeastType.EMPTY): Parser<TargetContainer> {
    return Parser.frame { r ->
        val expects = if (expect.isEmpty()) {
            TargetContainerParser.DEFAULT_PREFIX
        } else {
            expect
        }
        val parser = TargetContainerParser.parser(expects, r, type)
        Action { frame ->
            parser.process(frame)
        }
    }
}

fun ParserHolder.commandObjectiveOrSender(vararg expect: String): Parser<TargetContainer> {
    return commandObjective(arrayOf(*expect),LeastType.SENDER)
}

fun ParserHolder.commandObjectiveOrOrigin(vararg expect: String) : Parser<TargetContainer> {
    return commandObjective(arrayOf(*expect),LeastType.ORIGIN)
}

fun ParserHolder.commandObjectiveOrEmpty(vararg expect: String) : Parser<TargetContainer> {
    return commandObjective(arrayOf(*expect),LeastType.EMPTY)
}

fun ParserHolder.commandObjectiveOrConsole(vararg expect: String) : Parser<TargetContainer> {
    return commandObjective(arrayOf(*expect),LeastType.CONSOLE)
}

inline fun <reified T : Enum<T>> getEnumWithIdOrNull(name: String): T? {
    return T::class.java.enumConstants.firstOrNull { it.name.equals(name, ignoreCase = true) }
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

fun ParserHolder.commandVector(token: String, defaultValue: Vector = Vector(0, 0, 0)): Parser<Vector> {
    return command(token, then = actionVector()).option().defaultsTo(defaultValue)
}

/**
 * 模糊匹配 [] -> [] , "" -> []
 */
inline fun <reified T> ParserHolder.tokenListOf(uppercase: Boolean = true,crossinline block: (String) -> T): Parser<List<T>> {
    return Parser.frame { r ->
        val list = ArrayList<String>()
        try {
            r.mark()
            r.expect("[")
            while (r.hasNext() && r.peek() != ']') {
                val t = r.nextToken()
                list.add(if (uppercase) t.uppercase() else t)
            }
            r.expect("]")
        } catch (e: Exception) {
            r.reset()
            val t = r.nextToken()
            list.add(if (uppercase) t.uppercase() else t)
        }
        list.trimToSize()
        now {
            list.map { token -> block(token) }
        }
    }
}
