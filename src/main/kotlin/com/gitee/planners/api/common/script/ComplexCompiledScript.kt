package com.gitee.planners.api.common.script

import com.gitee.planners.core.skill.script.ScriptEventLoader
import taboolib.library.kether.Quest
import taboolib.module.kether.*
import taboolib.module.kether.Script
import java.util.*

interface ComplexCompiledScript {

    // 缓存唯一id(不可重复)
    val id: String

    // 是否异步运行
    val async: Boolean

    // 脚本源
    fun source(): String

    fun namespaces(): List<String>

    // 编译后的脚本
    fun compiledScript(): Script {
        if (!platform().getCache().scriptMap.containsKey(source())) {
            val complex = if (source().startsWith("def ")) source() else "def main = { ${source()} }"
            val quest = runKether(detailError = true) {
                complex.parseKetherScript(namespaces())
            }!!
            platform().getCache().scriptMap[source()] = quest
            ScriptEventLoader.registerListener(this@ComplexCompiledScript)
            return quest
        }

        return platform().getCache().scriptMap[source()]!!
    }

    fun getBlockScript(name: String): Optional<Quest.Block> {
        return compiledScript().getBlock(name)
    }

    // 缓存容器 编译隔离
    fun platform(): ComplexScriptPlatform

}
