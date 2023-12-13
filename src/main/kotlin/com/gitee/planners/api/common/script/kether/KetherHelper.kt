package com.gitee.planners.api.common.script.kether

import com.mojang.datafixers.kinds.App
import org.bukkit.Material
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.Parser
import taboolib.library.kether.QuestActionParser
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

/**
 * object的原因是供给外部使用
 */
object KetherHelper {

    const val NAMESPACE_COMMON = "planners-common"

    const val NAMESPACE_SKILL = "planners-skill"

    fun simpleKetherParser(vararg id: String, func: () -> ScriptActionParser<out Any?>): SimpleKetherParser {
        return object : SimpleKetherParser(*id) {
            override fun run(): ScriptActionParser<out Any?> {
                return func()
            }
        }
    }

    fun simpleKetherNow(vararg id: String, func: ScriptFrame.() -> Any?): SimpleKetherParser {
        return simpleKetherParser(*id) {
            scriptParser { actionNow { func(this) } }
        }
    }

    fun <T> combinedKetherParser(vararg id: String, builder: ParserHolder.(Parser.Instance) -> App<Parser.Mu, Parser.Action<T>>): SimpleKetherParser {
        return object : SimpleKetherParser(*id) {
            override fun run(): ScriptActionParser<Any?> {
                return ScriptActionParser {
                    Parser.build(builder(ParserHolder, Parser.instance())).resolve<Any?>(this)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun registerCombinationKetherParser(id: String, combinationKetherParser: CombinationKetherParser) {
        val ids = arrayOf(id, *combinationKetherParser.id)
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
        KetherLoader.registerParser(parser, id, namespace, true)
    }

    fun registerCombinationKetherParser(combinationKetherParser: CombinationKetherParser) {
        val id = combinationKetherParser.id
        val namespace = combinationKetherParser.namespace
        if (combinationKetherParser is KetherRegistry) {
            combinationKetherParser.onInit()
        }
        KetherLoader.registerParser(combinationKetherParser.run() as ScriptActionParser<*>, id, namespace, true)
    }

}
