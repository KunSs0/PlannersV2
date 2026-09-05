import com.novalang.runtime.NovaScheduler
import com.novalang.runtime.SchedulerHolder
import com.gitee.planners.module.script.PlannersCoreModule
import com.gitee.planners.module.script.ScriptManager
import com.novalang.workspace.ExecutionPolicy
import com.novalang.workspace.RuntimeWorkspace
import com.novalang.workspace.ScopeType
import com.novalang.workspace.SourceUnit
import com.novalang.workspace.WorkspaceHost
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.yaml.snakeyaml.Yaml
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

/**
 * Planners Nova Workspace 与通用默认 YAML 脚本的集成验收。
 */
class NovaWorkspaceIntegrationTest {

    @TempDir
    lateinit var workspaceRoot: Path

    /** 每个 Workspace 测试前注册 Planners 进程级共享模块。 */
    @BeforeEach
    fun registerCoreModule() {
        PlannersCoreModule.register()
    }

    /** 每个测试后释放进程级测试调度器。 */
    @AfterEach
    fun clearScheduler() {
        SchedulerHolder.clear()
        PlannersCoreModule.unregister()
    }

    /** 验证 Planners 核心模块可由虚拟入口显式导入并执行。 */
    @Test
    fun shouldLoadWorkspaceAndInvokeVirtualExpression() {
        installDirectScheduler()
        write(
            "script/nova.config.yml",
            "version: 1\n" +
                "name: planners-test\n" +
                "aliases:\n" +
                "  \"@planners\": \"planners\"\n" +
                "sources:\n" +
                "  - \"planners\"\n" +
                "entries:\n" +
                "  - \"@planners/bootstrap\"\n" +
                "runtime:\n" +
                "  security: trusted-server\n" +
                "  thread: main\n"
        )
        write(
            "script/planners/bootstrap.nova",
            "fun main() { }\n"
        )
        val virtual = SourceUnit(
            "@planners/generated/test-expression",
            "import \"planners.core\"\n\nfun evaluate(level) {\n    return level * 2\n}\n",
            workspaceRoot.resolve("config.yml"),
            "settings.test.expression",
            1,
            3,
            null
        )
        val host = WorkspaceHost { nova ->
            nova.setScriptClassLoader(NovaWorkspaceIntegrationTest::class.java.classLoader)
        }
        val workspace = RuntimeWorkspace(workspaceRoot.resolve("script/nova.config.yml"), host)
        workspace.registerVirtualSource(virtual, true, false)
        try {
            workspace.load()
            val scope = workspace.openScope(null, ScopeType.INVOCATION, "test-expression")
            try {
                val result = workspace.invoke(
                    virtual.moduleId,
                    "evaluate",
                    Collections.emptyMap<String, Any>(),
                    scope,
                    ExecutionPolicy.MAIN_THREAD,
                    4
                )
                assertEquals(8L, (result as Number).toLong())
            } finally {
                scope.dispose()
            }
        } finally {
            workspace.dispose()
        }
    }

    /** 遍历项目通用默认资源，并验证所有脚本块都能通过 Nova 语法编译。 */
    @Test
    fun shouldCompileEveryDefaultYamlScript() {
        installDirectScheduler()
        val sourceRoot = Path.of("src/main/resources").toAbsolutePath().normalize()
        val sourceStats = compileYamlRoot(sourceRoot, "project-resources")
        println(
            "Nova YAML audit: sourceFiles=${sourceStats.scannedFiles}, " +
                "sourceExecutableFiles=${sourceStats.executableFiles}, " +
                "sourceUnits=${sourceStats.sourceUnits}, semanticUnits=${sourceStats.semanticUnits}, " +
                "templateUnits=${sourceStats.templateUnits}, sourceBusinessLines=${sourceStats.businessLines}, " +
                "candidates=${sourceStats.candidateUnits}, excluded=${sourceStats.excludedReasons}, " +
                "categories=${sourceStats.semanticCategories}"
        )
        assertTrue(sourceStats.executableFiles > 0, "No executable default YAML files were compiled")
        assertTrue(sourceStats.sourceUnits > 0, "No default Nova SourceUnit was compiled")
    }

    /** 遍历一个配置根目录并编译其中全部 YAML 脚本节点。 */
    private fun compileYamlRoot(root: Path, workspaceName: String): CompilationStats {
        val auditRoot = workspaceRoot.resolve(workspaceName)
        prepareAuditWorkspace(auditRoot, workspaceName)
        val host = WorkspaceHost { nova ->
            nova.setScriptClassLoader(NovaWorkspaceIntegrationTest::class.java.classLoader)
        }
        val workspace = RuntimeWorkspace(auditRoot.resolve("script/nova.config.yml"), host)
        val stats = CompilationStats()
        val sequence = AtomicInteger(0)
        val files = Files.walk(root)
        try {
            val iterator = files.iterator()
            while (iterator.hasNext()) {
                val file = iterator.next()
                if (!Files.isRegularFile(file)) {
                    continue
                }
                val name = file.fileName.toString().lowercase()
                if (!name.endsWith(".yml") && !name.endsWith(".yaml")) {
                    continue
                }
                stats.scannedFiles += 1
                val previousUnits = stats.sourceUnits
                compileYamlFile(workspace, root, file, stats, sequence)
                if (stats.sourceUnits > previousUnits) {
                    stats.executableFiles += 1
                }
            }
        } finally {
            files.close()
        }
        try {
            workspace.load()
        } finally {
            workspace.dispose()
        }
        return stats
    }

    /** 解析并递归编译一个现网 YAML。 */
    private fun compileYamlFile(
        workspace: RuntimeWorkspace,
        root: Path,
        file: Path,
        stats: CompilationStats,
        sequence: AtomicInteger
    ) {
        val text = Files.readString(file, StandardCharsets.UTF_8)
        val decoded = Yaml().load<Any?>(text)
        val relative = root.relativize(file).toString().replace('\\', '/')
        compileValue(workspace, file, relative, emptyList(), decoded, false, stats, sequence)
    }

    /** 按生产收集器的节点语义递归生成并编译 Nova 源码。 */
    private fun compileValue(
        workspace: RuntimeWorkspace,
        originFile: Path,
        file: String,
        path: List<String>,
        value: Any?,
        insideVariables: Boolean,
        stats: CompilationStats,
        sequence: AtomicInteger
    ) {
        if (value is Map<*, *>) {
            for ((mapKey, mapValue) in value) {
                val childPath = ArrayList(path)
                childPath.add(mapKey.toString())
                compileValue(
                    workspace,
                    originFile,
                    file,
                    childPath,
                    mapValue,
                    insideVariables || mapKey.toString() == "variables",
                    stats,
                    sequence
                )
            }
            return
        }
        if (value is List<*>) {
            for (index in value.indices) {
                val childPath = ArrayList(path)
                childPath.add(index.toString())
                compileValue(workspace, originFile, file, childPath, value[index], insideVariables, stats, sequence)
            }
            return
        }
        if (path.isEmpty()) {
            return
        }
        val source: String
        if (value is String) {
            source = value
        } else if (insideVariables && (value is Number || value is Boolean)) {
            source = value.toString()
        } else {
            return
        }
        if (source.isBlank()) {
            if (isSemanticCandidate(path, insideVariables)) {
                stats.recordExcluded("blank-script")
            }
            return
        }
        val key = path.last()
        val id = "$file:${path.joinToString(".")}"
        val variableIndex = path.indexOf("variables")
        val variableDepth = if (variableIndex < 0) 0 else path.size - variableIndex - 1
        val variableExpression = insideVariables && (variableDepth == 1 || key == "condition" || key == "action")
        val actionEntryExpression = path.size >= 2 && path[path.size - 2] == "action"
        if (variableExpression) {
            registerExpression(workspace, originFile, id, source, sequence)
            stats.recordSemantic("variable", source)
        } else if (actionEntryExpression) {
            registerExpression(workspace, originFile, id, source, sequence)
            stats.recordSemantic("action-entry", source)
        } else if (key == "exper" || key == "experience" || key == "expression") {
            registerExpression(workspace, originFile, id, source, sequence)
            stats.recordSemantic(key, source)
        } else if (key == "consume") {
            registerAction(workspace, originFile, id, source, sequence)
            stats.recordSemantic("consume", source)
        } else if (key == "condition" && path.contains("hook")) {
            registerExpression(workspace, originFile, id, source, sequence)
            stats.recordSemantic("hook-condition", source)
        } else if (key == "action" || key == "script") {
            if (Regex("(?m)^\\s*fun\\s+").containsMatchIn(source)) {
                registerModule(workspace, originFile, id, source, sequence)
            } else {
                registerAction(workspace, originFile, id, source, sequence)
            }
            stats.recordSemantic(key, source)
        } else if (key == "condition") {
            stats.recordExcluded("ordinary-condition-scalar")
        } else if ((key == "if" || key == "post") && path.contains("condition")) {
            stats.recordExcluded("unused-route-condition-schema")
        }
        val templates = Regex("\\{\\{(.+?)}}").findAll(source)
        var templateIndex = 0
        for (match in templates) {
            registerExpression(workspace, originFile, "$id:template:$templateIndex", match.groupValues[1], sequence)
            stats.recordTemplate(match.groupValues[1])
            templateIndex += 1
        }
    }

    /** 判断空 scalar 是否属于业务解析器认可的脚本键。 */
    private fun isSemanticCandidate(path: List<String>, insideVariables: Boolean): Boolean {
        if (path.isEmpty()) {
            return false
        }
        val key = path.last()
        if (insideVariables && path.contains("variables")) {
            val variableIndex = path.indexOf("variables")
            val variableDepth = path.size - variableIndex - 1
            if (variableDepth == 1 || key == "condition" || key == "action") {
                return true
            }
        }
        if (path.size >= 2 && path[path.size - 2] == "action") {
            return true
        }
        if (key == "condition" && path.contains("hook")) {
            return true
        }
        return key == "exper" || key == "experience" || key == "expression" ||
            key == "consume" || key == "action" || key == "script"
    }

    /** 将表达式包装成独立 SourceUnit 并登记到 RuntimeWorkspace。 */
    private fun registerExpression(
        workspace: RuntimeWorkspace,
        originFile: Path,
        id: String,
        expression: String,
        sequence: AtomicInteger
    ) {
        val source = ScriptManager.createExpressionSource(emptyList(), expression)
        registerSource(workspace, originFile, id, source, ScriptManager.GENERATED_EXPRESSION_OFFSET, sequence)
    }

    /** 将语句包装成独立 SourceUnit 并登记到 RuntimeWorkspace。 */
    private fun registerAction(
        workspace: RuntimeWorkspace,
        originFile: Path,
        id: String,
        action: String,
        sequence: AtomicInteger
    ) {
        val source = ScriptManager.createActionSource(emptyList(), action)
        registerSource(workspace, originFile, id, source, ScriptManager.GENERATED_ACTION_OFFSET, sequence)
    }

    /** 将完整业务模块登记到 RuntimeWorkspace。 */
    private fun registerModule(
        workspace: RuntimeWorkspace,
        originFile: Path,
        id: String,
        action: String,
        sequence: AtomicInteger
    ) {
        val moduleId = createAuditModuleId(sequence, id)
        val source = ScriptManager.createModuleSource(action)
        val unit = SourceUnit(moduleId, source, originFile, id, 1, ScriptManager.GENERATED_MODULE_OFFSET, null)
        workspace.registerVirtualSource(unit, true, false)
    }

    /** 登记一个表达式或语句虚拟源。 */
    private fun registerSource(
        workspace: RuntimeWorkspace,
        originFile: Path,
        id: String,
        source: String,
        generatedLineOffset: Int,
        sequence: AtomicInteger
    ) {
        val moduleId = createAuditModuleId(sequence, id)
        val unit = SourceUnit(moduleId, source, originFile, id, 1, generatedLineOffset, null)
        workspace.registerVirtualSource(unit, true, false)
    }

    /** 生成一次测试 Workspace 内不冲突的虚拟模块 ID。 */
    private fun createAuditModuleId(sequence: AtomicInteger, id: String): String {
        val suffix = id.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return "@planners/generated/audit-${sequence.incrementAndGet()}-$suffix"
    }

    /** 写入仅包含 Planners 本体模块的审计 Workspace 配置。 */
    private fun prepareAuditWorkspace(root: Path, workspaceName: String) {
        writeAt(
            root,
            "script/planners/bootstrap.nova",
            "fun main() { }\n"
        )
        writeAt(
            root,
            "script/nova.config.yml",
            "version: 1\n" +
                "name: $workspaceName\n" +
                "aliases:\n" +
                "  \"@planners\": \"planners\"\n" +
                "sources:\n" +
                "  - \"planners\"\n" +
                "entries:\n" +
                "  - \"@planners/bootstrap\"\n" +
                "runtime:\n" +
                "  security: trusted-server\n" +
                "  thread: main\n"
        )
    }

    /** 在指定测试 Workspace 根目录写入 UTF-8 文件。 */
    private fun writeAt(root: Path, relative: String, content: String): Path {
        val target = root.resolve(relative)
        val parent = target.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        Files.writeString(target, content, StandardCharsets.UTF_8)
        return target
    }

    /** 将测试资源以 UTF-8 写入临时 Workspace。 */
    private fun write(relative: String, content: String): Path {
        val target = workspaceRoot.resolve(relative)
        val parent = target.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        Files.writeString(target, content, StandardCharsets.UTF_8)
        return target
    }

    /** 安装只在当前线程直接执行的 Nova 测试调度器。 */
    private fun installDirectScheduler() {
        val owner = Thread.currentThread()
        val executor = Executor { command ->
            command.run()
        }
        val scheduler = object : NovaScheduler {

            override fun mainExecutor(): Executor {
                return executor
            }

            override fun asyncExecutor(): Executor {
                return executor
            }

            override fun isMainThread(): Boolean {
                return Thread.currentThread() === owner
            }

            override fun scheduleLater(delayMs: Long, task: Runnable): NovaScheduler.Cancellable {
                throw UnsupportedOperationException("Delayed scheduling is not used by this test")
            }

            override fun scheduleRepeat(
                initialDelayMs: Long,
                periodMs: Long,
                task: Runnable
            ): NovaScheduler.Cancellable {
                throw UnsupportedOperationException("Repeating scheduling is not used by this test")
            }
        }
        SchedulerHolder.set(scheduler)
    }

    /** 单个 YAML 根目录的 Nova 业务源码审计计数。 */
    private class CompilationStats {

        var scannedFiles: Int = 0
        var executableFiles: Int = 0
        var sourceUnits: Int = 0
        var semanticUnits: Int = 0
        var templateUnits: Int = 0
        var businessLines: Int = 0
        var candidateUnits: Int = 0
        val semanticCategories = LinkedHashMap<String, Int>()
        val excludedReasons = LinkedHashMap<String, Int>()

        /** 记录一个语义节点虚拟 SourceUnit。 */
        fun recordSemantic(category: String, source: String) {
            candidateUnits += 1
            semanticUnits += 1
            val previous = semanticCategories[category]
            if (previous == null) {
                semanticCategories[category] = 1
            } else {
                semanticCategories[category] = previous + 1
            }
            record(source)
        }

        /** 记录一个模板表达式虚拟 SourceUnit。 */
        fun recordTemplate(source: String) {
            candidateUnits += 1
            templateUnits += 1
            record(source)
        }

        /** 记录一个脚本形态候选未进入 Workspace 的结构原因。 */
        fun recordExcluded(reason: String) {
            candidateUnits += 1
            val previous = excludedReasons[reason]
            if (previous == null) {
                excludedReasons[reason] = 1
            } else {
                excludedReasons[reason] = previous + 1
            }
        }

        /** 记录一个虚拟 SourceUnit 及其中非空业务源码行。 */
        private fun record(source: String) {
            sourceUnits += 1
            val lines = source.lines()
            for (line in lines) {
                if (line.isNotBlank()) {
                    businessLines += 1
                }
            }
        }
    }

}
