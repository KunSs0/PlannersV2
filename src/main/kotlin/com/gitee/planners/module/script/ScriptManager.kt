package com.gitee.planners.module.script

import com.novalang.workspace.ExecutionPolicy
import com.novalang.workspace.ResourceScope
import com.novalang.workspace.RuntimeWorkspace
import com.novalang.workspace.ScopeType
import com.novalang.workspace.SourceUnit
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

/**
 * Planners 插件级 Nova Workspace 的唯一生命周期所有者。
 *
 * 所有 YAML 表达式和 action 都必须先登记为虚拟 [SourceUnit]，再由 [load] 一次性
 * 构建模块图并预编译。Workspace 不存在 reload 中间态；业务重载只能先 [dispose]
 * 旧实例，再准备并加载一个新实例。
 */
object ScriptManager {

    private val lifecycleLock = Any()
    private val sources = LinkedHashMap<String, SourceUnit>()
    private val expressionUnits = LinkedHashMap<String, NovaScriptUnit>()
    private val skillExpressionUnits = LinkedHashMap<String, NovaScriptUnit>()
    private val invocationSequence = AtomicLong(0L)
    private var workspace: RuntimeWorkspace? = null
    private var persistentScope: ResourceScope? = null
    private var loaded = false

    /**
     * 创建尚未加载的新 Workspace 实例。
     *
     * 调用方必须先显式 [dispose] 旧实例；本方法拒绝覆盖已有实例。
     */
    @JvmStatic
    fun prepare() {
        synchronized(lifecycleLock) {
            if (workspace != null) {
                throw IllegalStateException("The Planners Nova Workspace has already been prepared")
            }
            releaseResourceFile("script/nova.config.yml", false)
            releaseResourceFile("script/planners/bootstrap.nova", false)
            val configFile = getDataFolder().toPath().resolve("script/nova.config.yml")
            val candidate = RuntimeWorkspace(configFile, PlannersWorkspaceHost)
            workspace = candidate
            loaded = false
        }
    }

    /**
     * 预编译全部已登记物理与虚拟入口，并发布持久资源作用域。
     */
    @JvmStatic
    fun load() {
        synchronized(lifecycleLock) {
            if (loaded) {
                return
            }
            val candidate = workspace
            if (candidate == null) {
                throw IllegalStateException("The Planners Nova Workspace must be prepared before load")
            }
            try {
                for (source in sources.values) {
                    candidate.registerVirtualSource(source, true, false)
                }
                candidate.load()
                persistentScope = candidate.openScope(null, ScopeType.PERSISTENT_REGISTRATION, "planners:persistent")
                loaded = true
            } catch (exception: RuntimeException) {
                workspace = null
                persistentScope = null
                loaded = false
                candidate.dispose()
                throw exception
            }
        }
    }

    /**
     * 销毁当前 Workspace 以及其下所有调度任务、业务实例与调用资源。
     */
    @JvmStatic
    fun dispose() {
        synchronized(lifecycleLock) {
            val current = workspace
            workspace = null
            persistentScope = null
            loaded = false
            sources.clear()
            expressionUnits.clear()
            skillExpressionUnits.clear()
            if (current != null) {
                current.dispose()
            }
        }
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
     * @return 以 main 为默认导出函数的入口描述。
     */
    @JvmStatic
    fun compileModule(id: String, source: String): NovaScriptUnit {
        val moduleId = createModuleId(id, source)
        val body = createModuleSource(source)
        return registerResolved(id, moduleId, "main", body, source)
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
        synchronized(lifecycleLock) {
            if (!expressionUnits.containsKey(expression)) {
                expressionUnits[expression] = unit
            }
        }
    }

    /** 按真实 YAML 来源登记只接收强类型技能执行上下文的表达式。 */
    fun registerYamlSkillExpression(file: Path, yamlPath: String, originLine: Int, expression: String) {
        val source = createExpressionSource(listOf("execution"), expression)
        val unit = registerYaml(file, yamlPath, originLine, "evaluate", source, GENERATED_EXPRESSION_OFFSET)
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

    /** 取得 YAML 收集阶段已经登记的强类型技能表达式入口。 */
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
        registerYaml(file, yamlPath, originLine, "main", body, GENERATED_MODULE_OFFSET, moduleId)
    }

    /** 在持久作用域内执行只接收显式参数的纯函数入口。 */
    @JvmStatic
    fun invokePure(unit: NovaScriptUnit, vararg args: Any?): Any? {
        ensureLoaded()
        val activeWorkspace = requireWorkspace()
        val scope = persistentScope
        if (scope == null) {
            throw IllegalStateException("The Planners persistent Nova scope is not active")
        }
        return invoke(activeWorkspace, scope, unit, emptyMap(), ExecutionPolicy.SERIAL_SCOPE, args)
    }

    /** 执行纯函数入口并记录完整宿主调用耗时。 */
    @JvmStatic
    fun invokePureProfiled(unit: NovaScriptUnit, vararg args: Any?): PureInvocation {
        val started = System.nanoTime()
        val value = invokePure(unit, *args)
        val elapsed = System.nanoTime() - started
        return PureInvocation(value, elapsed)
    }

    /** 在独立业务资源作用域内执行可创建任务或回调的入口。 */
    @JvmStatic
    fun invokeBusiness(
        unit: NovaScriptUnit,
        functionName: String,
        bindings: Map<String, Any?>,
        vararg args: Any?
    ): Any? {
        ensureLoaded()
        val activeWorkspace = requireWorkspace()
        val ownerId = "business#${invocationSequence.incrementAndGet()}"
        val scope = activeWorkspace.openScope(null, ScopeType.BUSINESS_INSTANCE, ownerId)
        try {
            val selected = NovaScriptUnit(unit.moduleId, functionName)
            return invoke(activeWorkspace, scope, selected, bindings, ExecutionPolicy.MAIN_THREAD, args)
        } finally {
            if (scope.resourceCount == 0) {
                scope.dispose()
            }
        }
    }

    /** 注册虚拟源码并拒绝加载后的运行期编译。 */
    private fun register(
        id: String,
        functionName: String,
        source: String,
        originSource: String,
        generatedLineOffset: Int
    ): NovaScriptUnit {
        synchronized(lifecycleLock) {
            val moduleId = createModuleId(id, source)
            if (sources.containsKey(moduleId)) {
                return NovaScriptUnit(moduleId, functionName)
            }
            if (loaded) {
                throw IllegalStateException("Nova sources must be registered before the Planners Workspace is loaded: $id")
            }
            val origin = locateOrigin(id, originSource)
            val sourceUnit = SourceUnit(moduleId, source, origin.file, id, origin.line, generatedLineOffset, null)
            sources[moduleId] = sourceUnit
            return NovaScriptUnit(moduleId, functionName)
        }
    }

    /** 登记已确定模块 ID 的完整 Nova 业务模块。 */
    private fun registerResolved(
        id: String,
        moduleId: String,
        functionName: String,
        source: String,
        originSource: String
    ): NovaScriptUnit {
        synchronized(lifecycleLock) {
            if (sources.containsKey(moduleId)) {
                return NovaScriptUnit(moduleId, functionName)
            }
            if (loaded) {
                throw IllegalStateException("Nova sources must be registered before the Planners Workspace is loaded: $id")
            }
            val origin = locateOrigin(id, originSource)
            val sourceUnit = SourceUnit(moduleId, source, origin.file, id, origin.line, GENERATED_MODULE_OFFSET, null)
            sources[moduleId] = sourceUnit
            return NovaScriptUnit(moduleId, functionName)
        }
    }

    /** 登记带精确 YAML SourceMap 的虚拟源码。 */
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
                throw IllegalStateException("Nova YAML sources must be registered before Workspace load: $id")
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

    /** 创建数据目录内 YAML 节点的稳定来源标识。 */
    private fun createYamlSourceId(file: Path, yamlPath: String): String {
        val root = getDataFolder().toPath().toAbsolutePath().normalize()
        val normalizedFile = file.toAbsolutePath().normalize()
        if (!normalizedFile.startsWith(root)) {
            throw IllegalArgumentException("Nova YAML source must be inside the Planners data folder: $normalizedFile")
        }
        val relativeFile = root.relativize(normalizedFile).toString().replace('\\', '/')
        return "$relativeFile:$yamlPath"
    }

    /** 调用 Workspace，并把业务绑定直接交给 Nova 执行上下文。 */
    private fun invoke(
        activeWorkspace: RuntimeWorkspace,
        scope: ResourceScope,
        unit: NovaScriptUnit,
        bindings: Map<String, Any?>,
        policy: ExecutionPolicy,
        args: Array<out Any?>
    ): Any? {
        @Suppress("UNCHECKED_CAST")
        val safeBindings = bindings as Map<String, Any>
        return activeWorkspace.invoke(unit.moduleId, unit.functionName, safeBindings, scope, policy, *args)
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

    /** 完整业务模块直接使用 YAML 正文，保留其中显式声明的 import。 */
    internal fun createModuleSource(source: String): String {
        return source
    }

    /** 按声明顺序写入 Nova 函数参数。 */
    private fun appendParameters(body: StringBuilder, parameters: List<String>) {
        for (index in parameters.indices) {
            if (index > 0) {
                body.append(", ")
            }
            body.append(parameters[index])
        }
    }

    /** 表达式包装中正文所在的生成行偏移。 */
    internal const val GENERATED_EXPRESSION_OFFSET = 2

    /** 语句包装中正文所在的生成行偏移。 */
    internal const val GENERATED_ACTION_OFFSET = 1

    /** 完整模块不产生前置源码行。 */
    internal const val GENERATED_MODULE_OFFSET = 0

    /** 确保业务调用前已完成启动期预编译，禁止运行期隐式加载。 */
    private fun ensureLoaded() {
        if (!loaded) {
            throw IllegalStateException("The Planners Nova Workspace has not completed startup loading")
        }
    }

    /** 获取当前活跃 Workspace。 */
    private fun requireWorkspace(): RuntimeWorkspace {
        val active = workspace
        if (active == null) {
            throw IllegalStateException("The Planners Nova Workspace is not active")
        }
        return active
    }

    /** 生成稳定且满足 Alias 约束的虚拟模块名。 */
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
     * @param id 业务虚拟源标识。
     * @param source 原始表达式、语句或模块文本。
     * @return 可供 Workspace 异常映射使用的来源位置。
     */
    private fun locateOrigin(id: String, source: String): SourceOrigin {
        val root = getDataFolder().toPath().toAbsolutePath().normalize()
        val candidates = ArrayList<Path>()
        val stream = Files.walk(root)
        try {
            val iterator = stream.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (!Files.isRegularFile(candidate)) {
                    continue
                }
                val fileName = candidate.fileName.toString().lowercase()
                if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                    candidates.add(candidate)
                }
            }
        } finally {
            stream.close()
        }
        val meaningfulLines = source.lines()
        var marker = ""
        for (line in meaningfulLines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("import ") && !trimmed.startsWith("fun evaluate")) {
                marker = trimmed.removeSuffix(";")
                break
            }
        }
        for (candidate in candidates) {
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
            for (candidate in candidates) {
                if (candidate.fileName.toString().equals(expectedName, true)) {
                    return SourceOrigin(candidate, 1)
                }
            }
        }
        val configFile = root.resolve("config.yml")
        if (Files.isRegularFile(configFile)) {
            return SourceOrigin(configFile, 1)
        }
        throw IllegalStateException("Cannot locate the YAML origin for Nova source: $id")
    }

    /** 虚拟源码对应的真实 YAML 位置。 */
    private class SourceOrigin(
        val file: Path,
        val line: Int
    )

    /** 一次预编译纯函数调用的宿主侧统计。 */
    class PureInvocation(
        val value: Any?,
        val elapsedNanos: Long
    )
}
