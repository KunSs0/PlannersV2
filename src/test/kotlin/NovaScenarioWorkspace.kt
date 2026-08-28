import com.novalang.runtime.Nova
import com.novalang.runtime.NovaScheduler
import com.novalang.runtime.SchedulerHolder
import com.novalang.workspace.ExecutionPolicy
import com.novalang.workspace.RuntimeWorkspace
import com.novalang.workspace.ScopeType
import com.novalang.workspace.SourceUnit
import com.novalang.workspace.WorkspaceHost
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashMap
import java.util.concurrent.Executor

/**
 * 业务场景测试共用的最小 Nova RuntimeWorkspace 夹具。
 *
 * 该夹具只提供配置、虚拟 SourceUnit、绑定与调用机制，不包含任何 Planners 业务规则。
 */
class NovaScenarioWorkspace(private val root: Path) : AutoCloseable {

    private val workspace: RuntimeWorkspace
    private var loaded = false

    init {
        installDirectScheduler()
        write(
            "script/nova.config.yml",
            "version: 1\n" +
                "name: planners-scenario-test\n" +
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
        write("script/libs/economy.api.nova", "fun identity(value) = value\n")
        write(
            "script/planners/bootstrap.nova",
            "fun main() { }\n"
        )
        val host = WorkspaceHost { nova: Nova ->
            nova.setScriptClassLoader(NovaScenarioWorkspace::class.java.classLoader)
        }
        workspace = RuntimeWorkspace(root.resolve("script/nova.config.yml"), host)
    }

    /**
     * 在 Workspace 加载前登记一个测试业务模块。
     *
     * @param moduleId 必须以 `@planners/` 开头的模块标识。
     * @param source 完整 Nova 源码。
     */
    fun register(moduleId: String, source: String) {
        if (loaded) {
            throw IllegalStateException("Nova test modules must be registered before Workspace load")
        }
        val unit = SourceUnit(moduleId, source, root.resolve("scenario.yml"), moduleId, 1, 0, null)
        workspace.registerVirtualSource(unit, true)
    }

    /** 完成全部测试模块的统一预编译。 */
    fun load() {
        workspace.load()
        loaded = true
    }

    /**
     * 调用已预编译的 Nova 测试函数。
     *
     * @param moduleId 模块标识。
     * @param functionName 导出函数名。
     * @param bindings 隔离绑定。
     * @param arguments 函数参数。
     * @return Nova 返回值。
     */
    fun invoke(
        moduleId: String,
        functionName: String,
        bindings: Map<String, Any> = emptyMap(),
        vararg arguments: Any?
    ): Any? {
        if (!loaded) {
            throw IllegalStateException("The Nova test Workspace has not been loaded")
        }
        val scope = workspace.openScope(null, ScopeType.INVOCATION, "scenario:$moduleId:$functionName")
        try {
            val safeBindings = LinkedHashMap<String, Any>()
            safeBindings.putAll(bindings)
            return workspace.invoke(
                moduleId,
                functionName,
                safeBindings,
                scope,
                ExecutionPolicy.MAIN_THREAD,
                *arguments
            )
        } finally {
            scope.dispose()
        }
    }

    /** 销毁 Workspace 资源树并释放进程级测试调度器。 */
    override fun close() {
        workspace.dispose()
        SchedulerHolder.clear()
    }

    /** 将 Workspace 物理文件以 UTF-8 写入临时目录。 */
    private fun write(relative: String, content: String) {
        val target = root.resolve(relative)
        val parent = target.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        Files.writeString(target, content, StandardCharsets.UTF_8)
    }

    /** 安装同步直执行调度器，验证 MAIN_THREAD 策略而不引入服务器线程。 */
    private fun installDirectScheduler() {
        SchedulerHolder.clear()
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
                throw UnsupportedOperationException("Delayed scheduling is not used by scenario tests")
            }

            override fun scheduleRepeat(
                initialDelayMs: Long,
                periodMs: Long,
                task: Runnable
            ): NovaScheduler.Cancellable {
                throw UnsupportedOperationException("Repeating scheduling is not used by scenario tests")
            }
        }
        SchedulerHolder.set(scheduler)
    }
}
