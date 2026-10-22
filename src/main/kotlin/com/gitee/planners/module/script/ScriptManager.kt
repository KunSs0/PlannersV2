package com.gitee.planners.module.script

import com.novalang.bukkit.NovaBukkit
import com.novalang.runtime.compilation.BytecodeArtifactCache
import com.novalang.runtime.compilation.SourceUnit
import taboolib.platform.BukkitPlugin
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.platform.function.releaseResourceFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Planners 插件级 Nova 编译程序的唯一生命周期所有者。
 *
 * 所有 YAML 表达式和 action 都必须先登记为虚拟 [SourceUnit]，再由 [load] 一次性
 * 构建模块图并预编译。编译程序不存在 reload 中间态；业务重载只能先 [close]
 * 旧实例，再准备并加载一个新实例。
 */
object ScriptManager {

    /** 单个插件代际最多缓存的模块编译组数量，防止频繁改脚本时无限累积。 */
    private const val MAX_CACHED_MODULE_GROUPS = 4096

    private val lifecycleLock = Any()
    private val sources = LinkedHashMap<String, SourceUnit>()
    private val expressionUnits = LinkedHashMap<String, NovaScriptUnit>()
    private val skillExpressionUnits = LinkedHashMap<String, NovaScriptUnit>()
    /** YAML 收集阶段得到的源码正文到原始位置的索引，避免重复扫描整个数据目录。 */
    private val yamlOrigins = ConcurrentHashMap<String, SourceOrigin>()
    /** 文件名到 YAML 路径的索引，仅在没有精确源码位置时按业务 ID 回退查找。 */
    @Volatile
    private var yamlFileIndex: YamlFileIndex? = null
    private val originContentHits = AtomicLong()
    private val originFileHits = AtomicLong()
    private val originScans = AtomicLong()
    private val originScanNanos = AtomicLong()
    private val originLookupNanos = AtomicLong()
    /**
     * 跨重载复用未变更模块的字节码产物。
     *
     * 产物只包含不可变字节码，每次加载仍创建独立类加载器，因此不会共享脚本静态状态。
     */
    private val bytecodeCache = BytecodeArtifactCache()
    @Volatile
    private var program: PlannersScriptProgram? = null
    private var configFile: Path? = null
    @Volatile
    private var loaded = false

    /**
     * 创建尚未加载的新编译程序。
     *
     * 调用方必须先显式 [close] 旧实例；本方法拒绝覆盖已有实例。
     */
    @JvmStatic
    fun prepare() {
        synchronized(lifecycleLock) {
            if (configFile != null) {
                throw IllegalStateException("The Planners Nova program has already been prepared")
            }
            releaseResourceFile("script/nova.config.yml", false)
            releaseResourceFile("script/planners/bootstrap.nova", false)
            configFile = getDataFolder().toPath().resolve("script/nova.config.yml")
            loaded = false
        }
    }

    /**
     * 统一预编译并初始化全部模块，成功后发布当前程序。
     *
     * @throws IllegalStateException 未准备编译配置时抛出。
     */
    @JvmStatic
    fun load() {
        synchronized(lifecycleLock) {
            if (loaded) {
                return
            }
            val file = configFile
            check(file != null) { "The Planners Nova program must be prepared before load" }
            // 缓存按模块源码指纹复用；超过上限时整体重建，避免长期开发重载无限累积字节码。
            if (bytecodeCache.size() > MAX_CACHED_MODULE_GROUPS) {
                bytecodeCache.clear()
                info("Nova bytecode cache exceeded $MAX_CACHED_MODULE_GROUPS entries; rebuilt from scratch")
            }
            val candidate = PlannersScriptProgram.compile(file, sources.values, bytecodeCache) { nova ->
                nova.setScriptClassLoader(ScriptManager::class.java.classLoader)
                NovaBukkit.install(nova, BukkitPlugin.getInstance())
            }
            candidate.initialize()
            program = candidate
            loaded = true
        }
    }

    /**
     * 关闭当前程序及其持有的所有任务、监听器和业务回调。
     */
    @JvmStatic
    fun close() {
        synchronized(lifecycleLock) {
            val current = program
            program = null
            configFile = null
            loaded = false
            sources.clear()
            expressionUnits.clear()
            skillExpressionUnits.clear()
            yamlOrigins.clear()
            yamlFileIndex = null
            originContentHits.set(0L)
            originFileHits.set(0L)
            originScans.set(0L)
            originScanNanos.set(0L)
            originLookupNanos.set(0L)
            if (current != null) {
                current.close()
            }
        }
    }

    /**
     * 释放跨重载复用的字节码缓存；插件停用时调用，重载本身保留缓存。
     */
    @JvmStatic
    fun clearCompilationCache() {
        bytecodeCache.clear()
    }

    /**
     * 登记一个 Nova 表达式入口。
     *
     * @param id 业务入口的稳定来源标识。
     * @param expression Nova 表达式正文。
     * @return 启动期登记后可供业务调用的入口描述。
     */
    @JvmStatic
    fun compileExpression(id: String, expression: String): NovaScriptUnit {
        return compileExpression(id, emptyList(), expression)
    }

    /**
     * 登记一个带显式参数的 Nova 表达式入口。
     *
     * @param id 业务入口的稳定来源标识。
     * @param parameters Nova 函数的显式形参名称。
     * @param expression Nova 表达式正文。
     * @return 启动期登记后可供业务调用的入口描述。
     */
    @JvmStatic
    fun compileExpression(id: String, parameters: List<String>, expression: String): NovaScriptUnit {
        val source = createExpressionSource(parameters, expression)
        return register(id, "evaluate", source, expression, GENERATED_EXPRESSION_OFFSET)
    }

    /**
     * 登记一个 Nova 语句入口。
     *
     * @param id 业务入口的稳定来源标识。
     * @param action Nova 语句正文。
     * @return 启动期登记后可供业务调用的入口描述。
     */
    @JvmStatic
    fun compileAction(id: String, action: String): NovaScriptUnit {
        return compileAction(id, emptyList(), action)
    }

    /**
     * 登记一个带显式参数的 Nova 语句入口。
     *
     * @param id 业务入口的稳定来源标识。
     * @param parameters Nova 函数的显式形参名称。
     * @param action Nova 语句正文。
     * @return 启动期登记后可供业务调用的入口描述。
     */
    @JvmStatic
    fun compileAction(id: String, parameters: List<String>, action: String): NovaScriptUnit {
        val source = createActionSource(parameters, action)
        return register(id, "evaluate", source, action, GENERATED_ACTION_OFFSET)
    }

    /**
     * 登记包含 execute/onHit 等业务导出函数的完整 Nova 模块。
     *
     * @param id 业务入口的稳定来源标识。
     * @param source 完整 Nova 业务模块正文。
     * @return 以 execute 为默认导出函数的入口描述。
     */
    @JvmStatic
    fun compileModule(id: String, source: String): NovaScriptUnit {
        val moduleId = createModuleId(id, source)
        val body = createModuleSource(source)
        return registerResolved(id, moduleId, "execute", body, source)
    }

    /**
     * 按真实 YAML 来源登记一个表达式虚拟源。
     *
     * @param file YAML 文件的绝对或可归一化路径。
     * @param yamlPath 脚本节点的 YAML 路径。
     * @param originLine 脚本节点的一基起始行号。
     * @param expression Nova 表达式正文。
     */
    fun registerYamlExpression(file: Path, yamlPath: String, originLine: Int, expression: String) {
        val source = createExpressionSource(emptyList(), expression)
        val unit = registerYaml(file, yamlPath, originLine, "evaluate", source, GENERATED_EXPRESSION_OFFSET)
        rememberOrigin(expression, file, originLine)
        synchronized(lifecycleLock) {
            if (!expressionUnits.containsKey(expression)) {
                expressionUnits[expression] = unit
            }
        }
    }

    /**
     * 按真实 YAML 来源登记只接收强类型技能执行上下文的表达式。
     */
    fun registerYamlSkillExpression(file: Path, yamlPath: String, originLine: Int, expression: String) {
        val source = createExpressionSource(listOf("execution"), expression)
        val unit = registerYaml(file, yamlPath, originLine, "evaluate", source, GENERATED_EXPRESSION_OFFSET)
        rememberOrigin(expression, file, originLine)
        synchronized(lifecycleLock) {
            if (!skillExpressionUnits.containsKey(expression)) {
                skillExpressionUnits[expression] = unit
            }
        }
    }

    /**
     * 取得 YAML 收集阶段已经登记的表达式入口。
     *
     * 外部业务扩展只能引用启动期已收集的表达式，不能在业务调用阶段临时编译源码。
     *
     * @param expression YAML 中的原始 Nova 表达式正文。
     * @return 对应的启动期预编译入口。
     * @throws IllegalStateException 表达式没有被 YAML 收集器登记时抛出。
     */
    @JvmStatic
    fun requirePrecompiledExpression(expression: String): NovaScriptUnit {
        synchronized(lifecycleLock) {
            val unit = expressionUnits[expression]
            if (unit == null) {
                throw IllegalStateException("The Nova expression was not precompiled: $expression")
            }
            return unit
        }
    }

    /**
     * 取得 YAML 收集阶段已经登记的强类型技能表达式入口。
     */
    @JvmStatic
    fun requirePrecompiledSkillExpression(expression: String): NovaScriptUnit {
        synchronized(lifecycleLock) {
            val unit = skillExpressionUnits[expression]
            if (unit == null) {
                throw IllegalStateException("The Nova skill expression was not precompiled: $expression")
            }
            return unit
        }
    }

    /**
     * 按真实 YAML 来源登记一个语句虚拟源。
     *
     * @param file YAML 文件的绝对或可归一化路径。
     * @param yamlPath 脚本节点的 YAML 路径。
     * @param originLine 脚本节点的一基起始行号。
     * @param action Nova 语句正文。
     */
    fun registerYamlAction(file: Path, yamlPath: String, originLine: Int, action: String) {
        val source = createActionSource(emptyList(), action)
        registerYaml(file, yamlPath, originLine, "evaluate", source, GENERATED_ACTION_OFFSET)
        rememberOrigin(action, file, originLine)
    }

    /**
     * 按真实 YAML 来源登记一个含导出函数的完整业务模块。
     *
     * @param file YAML 文件的绝对或可归一化路径。
     * @param yamlPath 脚本节点的 YAML 路径。
     * @param originLine 脚本节点的一基起始行号。
     * @param source 完整 Nova 业务模块正文。
     */
    fun registerYamlModule(file: Path, yamlPath: String, originLine: Int, source: String) {
        val id = createYamlSourceId(file, yamlPath)
        val moduleId = createModuleId(id, source)
        val body = createModuleSource(source)
        registerYaml(file, yamlPath, originLine, "execute", body, GENERATED_MODULE_OFFSET, moduleId)
        rememberOrigin(source, file, originLine)
    }

    /**
     * 在程序串行上下文中执行只接收显式参数的纯函数入口。
     */
    @JvmStatic
    fun invokePure(unit: NovaScriptUnit, vararg args: Any?): Any? {
        ensureLoaded()
        return requireProgram().invoke(unit, emptyMap(), *args)
    }

    /**
     * 执行纯函数入口并记录完整宿主调用耗时。
     */
    @JvmStatic
    fun invokePureProfiled(unit: NovaScriptUnit, vararg args: Any?): PureInvocation {
        val started = System.nanoTime()
        val value = invokePure(unit, *args)
        val elapsed = System.nanoTime() - started
        return PureInvocation(value, elapsed)
    }

    /**
     * 使用隔离绑定执行业务入口，创建的任务和回调归属当前程序。
     */
    @JvmStatic
    fun invokeBusiness(unit: NovaScriptUnit, functionName: String, bindings: Map<String, Any?>, vararg args: Any?): Any? {
        ensureLoaded()
        val selected = NovaScriptUnit(unit.moduleId, functionName)
        return requireProgram().invokeBusiness(selected, bindings, *args)
    }

    /**
     * 注册虚拟源码并拒绝加载后的运行期编译。
     */
    private fun register(id: String, functionName: String, source: String, originSource: String, generatedLineOffset: Int): NovaScriptUnit {
        synchronized(lifecycleLock) {
            val moduleId = createModuleId(id, source)
            if (sources.containsKey(moduleId)) {
                return NovaScriptUnit(moduleId, functionName)
            }
            if (loaded) {
                throw IllegalStateException("Nova sources must be registered before the Planners program is loaded: $id")
            }
            val origin = locateOrigin(id, originSource)
            val sourceUnit = SourceUnit(moduleId, source, origin.file, id, origin.line, generatedLineOffset, null)
            sources[moduleId] = sourceUnit
            return NovaScriptUnit(moduleId, functionName)
        }
    }

    /**
     * 登记已确定模块 ID 的完整 Nova 业务模块。
     */
    private fun registerResolved(id: String, moduleId: String, functionName: String, source: String, originSource: String): NovaScriptUnit {
        synchronized(lifecycleLock) {
            if (sources.containsKey(moduleId)) {
                return NovaScriptUnit(moduleId, functionName)
            }
            if (loaded) {
                throw IllegalStateException("Nova sources must be registered before the Planners program is loaded: $id")
            }
            val origin = locateOrigin(id, originSource)
            val sourceUnit = SourceUnit(moduleId, source, origin.file, id, origin.line, GENERATED_MODULE_OFFSET, null)
            sources[moduleId] = sourceUnit
            return NovaScriptUnit(moduleId, functionName)
        }
    }

    /**
     * 登记带精确 YAML SourceMap 的虚拟源码。
     */
    private fun registerYaml(
        file: Path,
        yamlPath: String,
        originLine: Int,
        functionName: String,
        source: String,
        generatedLineOffset: Int,
        resolvedModuleId: String? = null
    ): NovaScriptUnit {
        val id = createYamlSourceId(file, yamlPath)
        val normalizedFile = file.toAbsolutePath().normalize()
        synchronized(lifecycleLock) {
            val moduleId = if (resolvedModuleId == null) {
                createModuleId(id, source)
            } else {
                resolvedModuleId
            }
            if (sources.containsKey(moduleId)) {
                return NovaScriptUnit(moduleId, functionName)
            }
            if (loaded) {
                throw IllegalStateException("Nova YAML sources must be registered before program load: $id")
            }
            val sourceUnit = SourceUnit(
                moduleId,
                source,
                normalizedFile,
                yamlPath,
                originLine,
                generatedLineOffset,
                null
            )
            sources[moduleId] = sourceUnit
            return NovaScriptUnit(moduleId, functionName)
        }
    }

    /**
     * 创建数据目录内 YAML 节点的稳定来源标识。
     */
    private fun createYamlSourceId(file: Path, yamlPath: String): String {
        val root = getDataFolder().toPath().toAbsolutePath().normalize()
        val normalizedFile = file.toAbsolutePath().normalize()
        if (!normalizedFile.startsWith(root)) {
            throw IllegalArgumentException("Nova YAML source must be inside the Planners data folder: $normalizedFile")
        }
        val relativeFile = root.relativize(normalizedFile).toString().replace('\\', '/')
        return "$relativeFile:$yamlPath"
    }

    /**
     * 生成一个表达式虚拟模块。
     *
     * @param parameters evaluate 的显式形参。
     * @param expression YAML 中的 Nova 表达式正文。
     * @return 可直接登记为 SourceUnit 的完整 Nova 源码。
     */
    internal fun createExpressionSource(parameters: List<String>, expression: String): String {
        val body = StringBuilder()
        body.append("fun evaluate(")
        appendParameters(body, parameters)
        body.append(") {\n")
        body.append("    return ")
        body.append(expression)
        body.append("\n}\n")
        return body.toString()
    }

    /**
     * 生成一个语句虚拟模块。
     *
     * @param parameters evaluate 的显式形参。
     * @param action YAML 中的 Nova 语句正文。
     * @return 可直接登记为 SourceUnit 的完整 Nova 源码。
     */
    internal fun createActionSource(parameters: List<String>, action: String): String {
        val body = StringBuilder()
        body.append("fun evaluate(")
        appendParameters(body, parameters)
        body.append(") {\n")
        body.append(action)
        if (!action.endsWith('\n')) {
            body.append('\n')
        }
        body.append("}\n")
        return body.toString()
    }

    /**
     * 完整业务模块直接使用 YAML 正文，保留其中显式声明的 import。
     */
    internal fun createModuleSource(source: String): String {
        return source
    }

    /**
     * 按声明顺序写入 Nova 函数参数。
     */
    private fun appendParameters(body: StringBuilder, parameters: List<String>) {
        for (index in parameters.indices) {
            if (index > 0) {
                body.append(", ")
            }
            body.append(parameters[index])
        }
    }

    /**
     * 表达式包装中正文所在的生成行偏移。
     */
    internal const val GENERATED_EXPRESSION_OFFSET = 2

    /**
     * 语句包装中正文所在的生成行偏移。
     */
    internal const val GENERATED_ACTION_OFFSET = 1

    /**
     * 完整模块不产生前置源码行。
     */
    internal const val GENERATED_MODULE_OFFSET = 0

    /**
     * 确保业务调用前已完成启动期预编译，禁止运行期隐式加载。
     */
    private fun ensureLoaded() {
        if (!loaded) {
            throw IllegalStateException("The Planners Nova program has not completed startup loading")
        }
    }

    /**
     * 获取当前活跃编译程序。
     *
     * @return 当前程序。
     * @throws IllegalStateException 当前程序未加载时抛出。
     */
    private fun requireProgram(): PlannersScriptProgram {
        val active = program
        check(active != null) { "The Planners Nova program is not active" }
        return active
    }

    /**
     * 生成稳定且满足 Alias 约束的虚拟模块名。
     */
    private fun createModuleId(id: String, source: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((id + "\u0000" + source).toByteArray(StandardCharsets.UTF_8))
        val hash = bytes.take(8).joinToString("") { byte -> "%02x".format(byte) }
        val sanitized = id.lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-')
        val base = if (sanitized.isEmpty()) "source" else sanitized
        return "@planners/generated/$base-$hash"
    }

    /**
     * 将虚拟源码映射回真实 Planners YAML 文件和一基起始行。
     *
     * 优先使用 YAML 收集阶段建立的正文位置索引；业务构造器注册的源码正文通常
     * 与 YAML 节点完全一致，可以直接命中。只有合成源码（例如技能变量批处理）
     * 才按业务 ID 对应文件名回退，最后才扫描整个数据目录。
     *
     * @param id 业务虚拟源标识。
     * @param source 原始表达式、语句或模块文本。
     * @return 可供 编译程序 异常映射使用的来源位置。
     */
    private fun locateOrigin(id: String, source: String): SourceOrigin {
        val lookupStarted = System.nanoTime()
        try {
            val exact = yamlOrigins[source]
            if (exact != null) {
                originContentHits.incrementAndGet()
                return exact
            }
            val byFileName = locateOriginByFileName(id, source)
            if (byFileName != null) {
                originFileHits.incrementAndGet()
                return byFileName
            }
            originScans.incrementAndGet()
            val scanStarted = System.nanoTime()
            try {
                return scanOrigin(id, source)
            } finally {
                originScanNanos.addAndGet(System.nanoTime() - scanStarted)
            }
        } finally {
            originLookupNanos.addAndGet(System.nanoTime() - lookupStarted)
        }
    }

    /**
     * 按 `业务类型:ID:...` 中稳定的 ID 部分直接定位同名 YAML。
     *
     * ID 对应的文件存在时只读取该文件，不再遍历数据目录中的全部 YAML。
     *
     * @return 找到的来源位置；ID 没有稳定文件名时返回 null。
     */
    private fun locateOriginByFileName(id: String, source: String): SourceOrigin? {
        val idParts = id.split(':')
        if (idParts.size < 2) {
            return null
        }
        val expected = idParts[1].lowercase()
        val index = yamlFileIndex()
        val candidates = ArrayList<Path>()
        index.byName["$expected.yml"]?.let(candidates::addAll)
        index.byName["$expected.yaml"]?.let(candidates::addAll)
        if (candidates.isEmpty()) {
            return null
        }
        // 同名文件可能分布在 job/skill/state 等多个目录，优先选择业务类型对应的目录。
        val category = idParts[0].lowercase()
        val preferred = candidates.filter { candidate ->
            candidate.toString().replace('\\', '/').contains("/$category/")
        }
        val search = if (preferred.isEmpty()) candidates else preferred
        val marker = originMarker(source)
        for (candidate in search) {
            val lines = Files.readAllLines(candidate, StandardCharsets.UTF_8)
            if (marker.isNotEmpty()) {
                for (lineIndex in lines.indices) {
                    if (lines[lineIndex].contains(marker)) {
                        return SourceOrigin(candidate, lineIndex + 1)
                    }
                }
            }
        }
        return SourceOrigin(search.first(), 1)
    }

    /**
     * 在数据目录的全部 YAML 中查找正文标记；仅作为最后回退路径。
     */
    private fun scanOrigin(id: String, source: String): SourceOrigin {
        val marker = originMarker(source)
        for (candidate in yamlFileIndex().all) {
            val lines = Files.readAllLines(candidate, StandardCharsets.UTF_8)
            for (index in lines.indices) {
                if (marker.isNotEmpty() && lines[index].contains(marker)) {
                    return SourceOrigin(candidate, index + 1)
                }
            }
        }
        val idParts = id.split(':')
        if (idParts.size >= 2) {
            val expectedName = idParts[1] + ".yml"
            for (candidate in yamlFileIndex().all) {
                if (candidate.fileName.toString().equals(expectedName, true)) {
                    return SourceOrigin(candidate, 1)
                }
            }
        }
        val root = getDataFolder().toPath().toAbsolutePath().normalize()
        val configFile = root.resolve("config.yml")
        if (Files.isRegularFile(configFile)) {
            return SourceOrigin(configFile, 1)
        }
        throw IllegalStateException("Cannot locate the YAML origin for Nova source: $id")
    }

    /**
     * 提取源码中用于反向定位 YAML 的第一条有效行。
     */
    private fun originMarker(source: String): String {
        for (line in source.lines()) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("import ") && !trimmed.startsWith("fun evaluate")) {
                return trimmed.removeSuffix(";")
            }
        }
        return ""
    }

    /**
     * 记录 YAML 收集阶段得到的正文位置；重复正文保留第一次出现的位置。
     */
    private fun rememberOrigin(content: String, file: Path, line: Int) {
        if (content.isEmpty()) {
            return
        }
        yamlOrigins.putIfAbsent(content, SourceOrigin(file.toAbsolutePath().normalize(), line))
    }

    /**
     * 惰性建立当前数据目录的 YAML 文件名索引，重载关闭旧程序时失效。
     */
    private fun yamlFileIndex(): YamlFileIndex {
        yamlFileIndex?.let { return it }
        synchronized(lifecycleLock) {
            yamlFileIndex?.let { return it }
            val root = getDataFolder().toPath().toAbsolutePath().normalize()
            val byName = HashMap<String, MutableList<Path>>()
            val all = ArrayList<Path>()
            val stream = Files.walk(root)
            try {
                val iterator = stream.iterator()
                while (iterator.hasNext()) {
                    val candidate = iterator.next()
                    if (!Files.isRegularFile(candidate)) {
                        continue
                    }
                    val name = candidate.fileName.toString().lowercase()
                    if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                        byName.getOrPut(name) { ArrayList() }.add(candidate)
                        all.add(candidate)
                    }
                }
            } finally {
                stream.close()
            }
            for (candidates in byName.values) {
                candidates.sortBy { it.toString() }
            }
            all.sortBy { it.toString() }
            return YamlFileIndex(byName, all).also { yamlFileIndex = it }
        }
    }

    /**
     * 当前数据目录的 YAML 位置索引。
     */
    private class YamlFileIndex(val byName: Map<String, List<Path>>, val all: List<Path>)

    /**
     * 当前代际的虚拟源码与来源定位统计，用于启动、重载耗时诊断。
     */
    fun diagnostics(): NovaDiagnostics {
        val sourceCount = synchronized(lifecycleLock) { sources.size }
        return NovaDiagnostics(
            sources = sourceCount,
            contentHits = originContentHits.get(),
            fileHits = originFileHits.get(),
            scans = originScans.get(),
            scanNanos = originScanNanos.get(),
            lookupNanos = originLookupNanos.get(),
            cacheEntries = bytecodeCache.size()
        )
    }

    /**
     * 虚拟源码对应的真实 YAML 位置。
     */
    private class SourceOrigin(val file: Path, val line: Int)

    /**
     * 启动或重载阶段 Nova 来源定位的聚合统计。
     */
    class NovaDiagnostics(
        val sources: Int,
        val contentHits: Long,
        val fileHits: Long,
        val scans: Long,
        val scanNanos: Long,
        val lookupNanos: Long,
        val cacheEntries: Int
    ) {
        /** 按固定精度输出到生命周期耗时日志。 */
        override fun toString(): String {
            return "novaSources=$sources, novaCache=$cacheEntries, " +
                "origin(content=$contentHits, file=$fileHits, scan=$scans, " +
                "scanMs=${String.format(Locale.ROOT, "%.1f", scanNanos / 1_000_000.0)}, " +
                "lookupMs=${String.format(Locale.ROOT, "%.1f", lookupNanos / 1_000_000.0)})"
        }
    }

    /**
     * 一次预编译纯函数调用的宿主侧统计。
     */
    class PureInvocation(val value: Any?, val elapsedNanos: Long)
}
