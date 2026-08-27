import com.novalang.runtime.NovaScheduler
import com.novalang.runtime.SchedulerHolder
import com.gitee.planners.module.script.ScriptManager
import com.novalang.workspace.ExecutionPolicy
import com.novalang.workspace.RuntimeWorkspace
import com.novalang.workspace.ScopeType
import com.novalang.workspace.SourceUnit
import com.novalang.workspace.WorkspaceHost
import org.junit.jupiter.api.AfterEach
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
 * Planners Nova Workspace 与现网 YAML 脚本的集成验收。
 */
class NovaWorkspaceIntegrationTest {

    @TempDir
    lateinit var workspaceRoot: Path

    /** 每个测试后释放进程级测试调度器。 */
    @AfterEach
    fun clearScheduler() {
        SchedulerHolder.clear()
    }

    /** 验证 @planners 与 @nova Alias 均落在 sources 内并可执行虚拟入口。 */
    @Test
    fun shouldLoadWorkspaceAndInvokeVirtualExpression() {
        installDirectScheduler()
        write(
            "script/nova.config.yml",
            "version: 1\n" +
                "name: planners-test\n" +
                "aliases:\n" +
                "  \"@planners\": \"planners\"\n" +
                "  \"@nova\": \"libs\"\n" +
                "sources:\n" +
                "  - \"planners\"\n" +
                "  - \"libs\"\n" +
                "entries:\n" +
                "  - \"@planners/bootstrap\"\n" +
                "runtime:\n" +
                "  security: trusted-server\n" +
                "  thread: main\n"
        )
        write("script/libs/runtime.nova", "fun identity(value) = value\n")
        write("script/planners/lib/runtime.nova", "fun plannersRuntimeVersion(): Int = 1\n")
        write(
            "script/planners/bootstrap.nova",
            "import \"@nova/runtime\"\n" +
                "import \"@planners/lib/runtime\"\n\n" +
                "fun main() { identity(plannersRuntimeVersion()) }\n"
        )
        val virtual = SourceUnit(
            "@planners/generated/test-expression",
            "import \"@nova/runtime\"\n\nfun evaluate(level) {\n    return level * 2\n}\n",
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
        workspace.registerVirtualSource(virtual, true)
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

    /** 验证 WorkspaceTasks 任务归属业务 Scope，并在 Workspace dispose 时取消。 */
    @Test
    fun shouldDisposeScheduledWorkspaceTask() {
        val scheduler = CapturingScheduler(Thread.currentThread())
        SchedulerHolder.set(scheduler)
        prepareAuditWorkspace(workspaceRoot, "planners-task-test")
        val moduleId = "@planners/generated/task-test"
        val businessSource =
            "fun onLater() { }\n" +
                "fun execute() { Java.static(\"com.novalang.workspace.WorkspaceTasks\", \"later\", 50, " +
                "__workspaceEntry, \"onLater\") }\n"
        val source = ScriptManager.createModuleSource(moduleId, businessSource)
        val unit = SourceUnit(moduleId, source, workspaceRoot.resolve("skill/task.yml"), "skill.task.action", 1, 20, null)
        val host = WorkspaceHost { nova ->
            nova.setScriptClassLoader(NovaWorkspaceIntegrationTest::class.java.classLoader)
        }
        val workspace = RuntimeWorkspace(workspaceRoot.resolve("script/nova.config.yml"), host)
        workspace.registerVirtualSource(unit, true)
        workspace.load()
        val scope = workspace.openScope(null, ScopeType.BUSINESS_INSTANCE, "task-test")
        workspace.invoke(moduleId, "execute", Collections.emptyMap<String, Any>(), scope, ExecutionPolicy.MAIN_THREAD)
        assertEquals(1, scope.resourceCount)

        workspace.dispose()

        assertTrue(scheduler.handle.cancelled)
    }

    /** 遍历项目资源与现网 Planners 配置，并验证所有脚本块都能通过 Nova 语法编译。 */
    @Test
    fun shouldCompileEveryDeployedYamlScript() {
        installDirectScheduler()
        val configuredRoot = System.getProperty("planners.test.plannersRoot")
        val deploymentRoot: Path
        if (configuredRoot == null || configuredRoot.isBlank()) {
            deploymentRoot = Path.of("E:/temp/server-main/plugins/Planners")
        } else {
            deploymentRoot = Path.of(configuredRoot)
        }
        if (!Files.isDirectory(deploymentRoot)) {
            throw IllegalStateException("The deployed Planners configuration does not exist: $deploymentRoot")
        }
        val sourceRoot = Path.of("src/main/resources").toAbsolutePath().normalize()
        val sourceStats = compileYamlRoot(sourceRoot, "project-resources")
        val deploymentStats = compileYamlRoot(deploymentRoot, "deployed-resources")
        println(
            "Nova YAML audit: sourceFiles=${sourceStats.scannedFiles}, " +
                "sourceExecutableFiles=${sourceStats.executableFiles}, " +
                "sourceUnits=${sourceStats.sourceUnits}, semanticUnits=${sourceStats.semanticUnits}, " +
                "templateUnits=${sourceStats.templateUnits}, sourceBusinessLines=${sourceStats.businessLines}, " +
                "candidates=${sourceStats.candidateUnits}, excluded=${sourceStats.excludedReasons}, " +
                "categories=${sourceStats.semanticCategories}"
        )
        println(
            "Nova YAML audit: deploymentFiles=${deploymentStats.scannedFiles}, " +
                "deploymentExecutableFiles=${deploymentStats.executableFiles}, " +
                "deploymentUnits=${deploymentStats.sourceUnits}, semanticUnits=${deploymentStats.semanticUnits}, " +
                "templateUnits=${deploymentStats.templateUnits}, deploymentBusinessLines=${deploymentStats.businessLines}, " +
                "candidates=${deploymentStats.candidateUnits}, excluded=${deploymentStats.excludedReasons}, " +
                "categories=${deploymentStats.semanticCategories}"
        )
        assertTrue(deploymentStats.executableFiles > 0, "No executable deployed YAML files were compiled")
        assertTrue(deploymentStats.sourceUnits > 0, "No deployed Nova SourceUnit was compiled")
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
        val source = ScriptManager.createModuleSource(moduleId, action)
        val unit = SourceUnit(moduleId, source, originFile, id, 1, ScriptManager.GENERATED_MODULE_OFFSET, null)
        workspace.registerVirtualSource(unit, true)
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
        workspace.registerVirtualSource(unit, true)
    }

    /** 生成一次测试 Workspace 内不冲突的虚拟模块 ID。 */
    private fun createAuditModuleId(sequence: AtomicInteger, id: String): String {
        val suffix = id.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return "@planners/generated/audit-${sequence.incrementAndGet()}-$suffix"
    }

    /** 复制真实 Nova 库并写入 Alias 目标均位于 sources 内的测试配置。 */
    private fun prepareAuditWorkspace(root: Path, workspaceName: String) {
        writeAt(root, "script/libs/runtime.nova", Files.readString(Path.of("src/main/resources/script/libs/runtime.nova")))
        writeAt(
            root,
            "script/planners/lib/runtime.nova",
            Files.readString(Path.of("src/main/resources/script/planners/lib/runtime.nova"))
        )
        writeAt(
            root,
            "script/planners/bootstrap.nova",
            Files.readString(Path.of("src/main/resources/script/planners/bootstrap.nova"))
        )
        writeAt(
            root,
            "script/nova.config.yml",
            "version: 1\n" +
                "name: $workspaceName\n" +
                "aliases:\n" +
                "  \"@planners\": \"planners\"\n" +
                "  \"@nova\": \"libs\"\n" +
                "sources:\n" +
                "  - \"planners\"\n" +
                "  - \"libs\"\n" +
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

    /** 捕获一次延迟任务，用于验证 Workspace dispose 的资源释放。 */
    private class CapturingScheduler(private val owner: Thread) : NovaScheduler {

        val handle = CapturingHandle()

        override fun mainExecutor(): Executor {
            return Executor { command -> command.run() }
        }

        override fun asyncExecutor(): Executor {
            return Executor { command -> command.run() }
        }

        override fun isMainThread(): Boolean {
            return Thread.currentThread() === owner
        }

        override fun scheduleLater(delayMs: Long, task: Runnable): NovaScheduler.Cancellable {
            return handle
        }

        override fun scheduleRepeat(
            initialDelayMs: Long,
            periodMs: Long,
            task: Runnable
        ): NovaScheduler.Cancellable {
            throw UnsupportedOperationException("Repeating scheduling is not used by this test")
        }

        /** 可观察取消状态的调度句柄。 */
        class CapturingHandle : NovaScheduler.Cancellable {

            var cancelled: Boolean = false

            override fun cancel() {
                cancelled = true
            }

            override fun isCancelled(): Boolean {
                return cancelled
            }
        }
    }
}
