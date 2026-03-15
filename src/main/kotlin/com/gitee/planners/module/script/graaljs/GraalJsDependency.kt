package com.gitee.planners.module.script.graaljs

import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency

@RuntimeDependencies(
    RuntimeDependency(
        value = "!org.graalvm.polyglot:polyglot:24.1.1",
        test = "!org.graalvm.polyglot.Context",
        transitive = false
    ),
    RuntimeDependency(
        value = "!org.graalvm.js:js-language:24.1.1",
        test = "!com.oracle.truffle.js.lang.JavaScriptLanguage",
        transitive = false
    ),
    RuntimeDependency(
        value = "!org.graalvm.truffle:truffle-api:24.1.1",
        test = "!com.oracle.truffle.api.TruffleLanguage",
        transitive = false
    ),
    RuntimeDependency(
        value = "!org.graalvm.sdk:collections:24.1.1",
        test = "!org.graalvm.collections.EconomicMap",
        transitive = false
    ),
    RuntimeDependency(
        value = "!org.graalvm.sdk:nativeimage:24.1.1",
        test = "!org.graalvm.nativeimage.Platform",
        transitive = false
    ),
    RuntimeDependency(
        value = "!org.graalvm.sdk:word:24.1.1",
        test = "!org.graalvm.word.WordBase",
        transitive = false
    ),
    RuntimeDependency(
        value = "!org.graalvm.regex:regex:24.1.1",
        test = "!com.oracle.truffle.regex.RegexLanguage",
        transitive = false
    ),
    RuntimeDependency(
        value = "!org.graalvm.shadowed:icu4j:24.1.1",
        test = "!com.oracle.truffle.regex.tregex.string.Encodings",
        ignoreException = true,
        transitive = false
    ),
)
object GraalJsDependency
