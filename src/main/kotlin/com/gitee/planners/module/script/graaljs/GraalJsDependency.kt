package com.gitee.planners.module.script.graaljs

import taboolib.common.env.RuntimeEnv

/**
 * GraalVM Polyglot/JS 编程式依赖加载
 *
 * GraalJsEngine / GraalJsSession 标记 @Ghost 避免 ClassVisitorHandler 扫描时触发类加载。
 * Java < 17: Legacy 21.3.15
 * Java >= 17: Modern 24.1.1
 */
object GraalJsDependency {

    private val IS_MODERN = !System.getProperty("java.version").let { v ->
        if (v.startsWith("1.")) v.substring(2, 3).toInt() < 17 else v.substringBefore(".").toInt() < 17
    }

    init {
        val deps = if (IS_MODERN) arrayOf(
            "org.graalvm.polyglot:polyglot:24.1.1",
            "org.graalvm.js:js-language:24.1.1",
            "org.graalvm.truffle:truffle-api:24.1.1",
            "org.graalvm.sdk:collections:24.1.1",
            "org.graalvm.sdk:nativeimage:24.1.1",
            "org.graalvm.sdk:word:24.1.1",
            "org.graalvm.regex:regex:24.1.1",
            "org.graalvm.shadowed:icu4j:24.1.1",
        ) else arrayOf(
            "org.graalvm.sdk:graal-sdk:21.3.15",
            "org.graalvm.js:js:21.3.15",
            "org.graalvm.truffle:truffle-api:21.3.15",
            "org.graalvm.regex:regex:21.3.15",
        )
        deps.forEach { RuntimeEnv.ENV_DEPENDENCY.loadDependency(it) }
    }
}
