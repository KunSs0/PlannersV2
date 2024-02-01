package com.gitee.planners.api.common.script.kether

import com.mojang.datafixers.kinds.App
import taboolib.library.kether.*
import taboolib.module.kether.*

/**
 * object的原因是供给外部使用
 */
object KetherHelper {

    const val NAMESPACE_COMMON = "planners-common"

    const val NAMESPACE_SKILL = "planners-skill"

    fun simpleKetherParser(vararg id: String, func: (QuestReader) -> QuestAction<out Any?>): SimpleKetherParser {
        return createSimpleKetherParser(*id) {
            scriptParser { func(it) }
        }
    }

    fun createSimpleKetherParser(vararg id: String, func: () -> QuestActionParser): SimpleKetherParser {
        return object : SimpleKetherParser(*id) {
            override fun run(): QuestActionParser {
                return func()
            }
        }
    }

    fun simpleKetherNow(vararg id: String, func: ScriptFrame.() -> Any?): SimpleKetherParser {
        return simpleKetherParser(*id) {
            actionNow { func(this) }
        }
    }

    fun simpleKetherVoid(vararg id: String, func: ScriptFrame.() -> Unit): SimpleKetherParser {
        return simpleKetherNow(*id) {
            func(this)
            null
        }
    }

    fun combinedKetherParser(
        vararg id: String,
        builder: ParserHolder.(Parser.Instance) -> App<Parser.Mu, Parser.Action<Any?>>
    ): SimpleKetherParser {
        return object : SimpleKetherParser(*id) {

            override fun run(): ScriptActionParser<Any?> {
                return combinationParser(builder)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun registerCombinationKetherParser(id: String, combinationKetherParser: CombinationKetherParser) {
        // method 过滤方法名字以action开头的
        val ids = arrayOf(id, *combinationKetherParser.id).filter { !it.startsWith("action") }.toTypedArray()
        val namespace = combinationKetherParser.namespace
        if (combinationKetherParser is KetherRegistry) {
            combinationKetherParser.onInit()
        }
        registerCombinationKetherParser(ids, namespace, combinationKetherParser.run() as ScriptActionParser<Any?>)
    }

    fun registerCombinationKetherParser(id: String, namespace: String, parser: ScriptActionParser<Any?>) {
        registerCombinationKetherParser(arrayOf(id), namespace, parser)
    }

    fun registerCombinationKetherParser(id: Array<String>, namespace: String, parser: ScriptActionParser<Any?>) {
        println("register combination parser ${id.toList()} namespace $namespace ")
        KetherLoader.registerParser(parser, id, namespace, true)
    }


    fun registerCombinationKetherParser(combinationKetherParser: CombinationKetherParser) {
        val id = combinationKetherParser.id
        val namespace = combinationKetherParser.namespace
        if (combinationKetherParser is KetherRegistry) {
            combinationKetherParser.onInit()
        }
        this.registerCombinationKetherParser(id, namespace, combinationKetherParser.run() as ScriptActionParser<Any?>)
    }

}
