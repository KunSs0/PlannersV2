import com.novalang.runtime.NovaScheduler
import com.novalang.runtime.SchedulerHolder
import com.gitee.planners.module.script.NovaScriptUnit
import com.gitee.planners.module.script.PlannersScriptProgram
import com.novalang.runtime.compilation.SourceUnit
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor

/**
 * 业务场景测试共用的最小 Nova 编译程序夹具。
 *
 * 该夹具只提供配置、虚拟 SourceUnit、绑定与调用机制，不包含任何 Planners 业务规则。
 */
class NovaScenarioWorkspace(private val root: Path) : AutoCloseable {

    private val sources = ArrayList<SourceUnit>()
    private var program: PlannersScriptProgram? = null

    init {
        installDirectScheduler()
        write("script/nova.config.yml", "version: 1\nname: planners-test\naliases:\n  \"@planners\": \"planners\"\n")
        write("script/planners/bootstrap.nova", "fun main() { }\n")
    }

    /**
     * 在程序加载前登记一个测试业务模块。
     *
     * @param moduleId 必须以 `@planners/` 开头的模块标识。
     * @param source 完整 Nova 源码。
     */
    fun register(moduleId: String, source: String) {
        if (program != null) {
            throw IllegalStateException("Nova test modules must be registered before program load")
        }
        val unit = SourceUnit(moduleId, source, root.resolve("scenario.yml"), moduleId, 1, 0, null)
        sources.add(unit)
    }

    /**
     * 登记保留原始来源的测试模块。
     *
     * @param source 测试源码单元。
     */
    fun register(source: SourceUnit) {
        check(program == null) { "Test program is already loaded" }
        sources.add(source)
    }

    /**
     * 使用真实生产编译入口加载合成测试模块。
     */
    fun load() {
        check(program == null) { "Test program is already loaded" }
        val candidate = PlannersScriptProgram.compile(root.resolve("script/nova.config.yml"), sources) { nova ->
            nova.setScriptClassLoader(NovaScenarioWorkspace::class.java.classLoader)
        }
        candidate.initialize()
        program = candidate
    }

    /**
     * 调用带显式参数的纯函数。
     *
     * @param moduleId 模块标识。
     * @param functionName 函数名。
     * @param arguments 显式参数。
     * @return 脚本返回值。
     */
    fun invokePure(moduleId: String, functionName: String, vararg arguments: Any?): Any? {
        val active = program
        check(active != null) { "Test program is not loaded" }
        return active.invoke(NovaScriptUnit(moduleId, functionName), emptyMap(), *arguments)
    }

    /**
     * 在主线程边界执行隔离业务调用。
     *
     * @param moduleId 模块标识。
     * @param functionName 函数名。
     * @param bindings 本次业务绑定。
     * @param arguments 显式参数。
     * @return 脚本返回值。
     */
    fun invoke(moduleId: String, functionName: String, bindings: Map<String, Any?> = emptyMap(), vararg arguments: Any?): Any? {
        val active = program
        check(active != null) { "Test program is not loaded" }
        return active.invokeBusiness(NovaScriptUnit(moduleId, functionName), bindings, *arguments)
    }

    /**
     * 关闭程序并清理测试拥有的调度器。
     */
    override fun close() {
        val active = program
        program = null
        if (active != null) {
            active.close()
        }
        SchedulerHolder.clear()
    }

    /**
     * 将 程序 物理文件以 UTF-8 写入临时目录。
     */
    private fun write(relative: String, content: String) {
        val target = root.resolve(relative)
        val parent = target.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        Files.writeString(target, content, StandardCharsets.UTF_8)
    }

    /**
     * 安装同步直执行调度器，验证 MAIN_THREAD 策略而不引入服务器线程。
     */
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
