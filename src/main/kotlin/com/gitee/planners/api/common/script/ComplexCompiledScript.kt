package com.gitee.planners.api.common.script

import taboolib.module.kether.KetherShell
import taboolib.module.kether.Script
import taboolib.module.kether.parseKetherScript
import taboolib.module.kether.runKether
import java.util.concurrent.CompletableFuture

interface ComplexCompiledScript {

    // 缓存唯一id(不可重复)
    val id: String

    // 脚本源
    fun source(): String

    fun namespaces(): List<String>

    // 编译后的脚本
    fun compiledScript(): Script {
        return platform().getCache().scriptMap.computeIfAbsent(source()) {
            val complex = if (source().startsWith("def ")) source() else "def main = { ${source()} }"
            runKether {
                complex.parseKetherScript(namespaces())
            }!!
        }
    }

    // 缓存容器 编译隔离
    fun platform(): ComplexScriptPlatform

}
