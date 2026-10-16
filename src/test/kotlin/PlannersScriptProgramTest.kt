import com.gitee.planners.module.script.NovaScriptUnit
import com.gitee.planners.module.script.PlannersScriptProgram
import com.novalang.runtime.NovaScheduler
import com.novalang.runtime.SchedulerHolder
import com.novalang.runtime.ScriptCallback
import com.novalang.runtime.compilation.SourceUnit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * 验证真实 Planners 编译程序的初始化、绑定、回调和任务生命周期。
 */
class PlannersScriptProgramTest {
    @TempDir
    lateinit var root: Path
    private val scheduler = CapturingScheduler()

    /**
     * 为每个合成宿主测试安装独立调度器。
     */
    @BeforeEach
    fun prepare() {
        SchedulerHolder.clear()
        SchedulerHolder.set(scheduler)
        Files.writeString(root.resolve("nova.config.yml"), "version: 1\nname: test\naliases:\n  '@planners': '.'\n")
        Files.writeString(root.resolve("bootstrap.nova"), "object Counter { var value = 0; fun next(): Int { value = value + 1; return value } }")
    }

    /**
     * 释放本用例的宿主调度器。
     */
    @AfterEach
    fun cleanup() {
        SchedulerHolder.clear()
        scheduler.executor.shutdownNow()
    }

    /**
     * 加载不执行动作；同程序共享模块状态，不同程序隔离；绑定逐次隔离。
     */
    @Test
    fun actionsWaitForInvocationAndProgramsKeepIndependentState() {
        val first = compile()
        val second = compile()
        try {
            first.initialize()
            second.initialize()
            assertEquals(1, first.invokeBusiness(unit("execute"), emptyMap()))
            assertEquals(2, first.invokeBusiness(unit("execute"), emptyMap()))
            assertEquals(1, second.invokeBusiness(unit("execute"), emptyMap()))
            assertEquals("alice", first.invokeBusiness(unit("read"), mapOf("player" to "alice")))
            assertEquals("bob", first.invokeBusiness(unit("read"), mapOf("player" to "bob")))
            assertNull(first.invokeBusiness(unit("read"), emptyMap()))
        } finally {
            first.close()
            second.close()
        }
    }

    /**
     * 关闭旧程序取消任务和回调，即使已排队的旧 Runnable 再次触发也不能修改状态。
     */
    @Test
    fun closingProgramCancelsTasksAndInvalidatesCallbacks() {
        val program = compile()
        try {
            program.initialize()
            val callback = program.invokeBusiness(unit("callback"), emptyMap()) as ScriptCallback
            assertEquals("alice", callback.invokeWithBindings(mapOf("player" to "alice")))
            program.invokeBusiness(unit("later"), emptyMap())
            assertEquals(1, scheduler.tasks.size)
            program.close()
            assertFalse(callback.isValid)
            assertTrue(scheduler.tasks[0].cancelled)
            scheduler.tasks[0].action.run()
            assertThrows(IllegalStateException::class.java) { callback.invoke() }
            assertThrows(IllegalStateException::class.java) { program.invoke(unit("execute"), emptyMap()) }
        } finally {
            program.close()
        }
    }

    /**
     * 旧动作 main 必须直接报错，不在加载阶段误施法。
     */
    @Test
    fun rejectsReservedActionMain() {
        val source = SourceUnit("action", "fun main() { error(\"must not run\") }", root.resolve("skill.yml"), "action", 7, 0, null)
        val failure = assertThrows(IllegalArgumentException::class.java) {
            PlannersScriptProgram.compile(root.resolve("nova.config.yml"), listOf(source)) { }
        }
        assertTrue(failure.message!!.contains("skill.yml [action]:7"))
    }

    /**
     * 语法错误继续经过 Nova 编译器，错误必须包含原始 YAML 来源。
     */
    @Test
    fun parseFailureKeepsYamlOrigin() {
        val source = SourceUnit("invalid", "fun execute() { val = 1 }", root.resolve("broken.yml"), "action", 12, 0, null)
        val failure = assertThrows(RuntimeException::class.java) {
            PlannersScriptProgram.compile(root.resolve("nova.config.yml"), listOf(source)) { }
        }
        assertTrue(failure.message!!.contains("broken.yml"))
    }

    /**
     * 从异步线程进入的业务调用必须由宿主主线程执行。
     */
    @Test
    fun asyncInvocationUsesHostMainExecutor() {
        val program = compile()
        try {
            program.initialize()
            scheduler.mainThread = null
            val actual = program.invokeBusiness(unit("thread"), emptyMap())
            assertEquals("planners-host-main", actual)
        } finally {
            program.close()
        }
    }

    /**
     * 编译使用真实生产入口的合成模块。
     *
     * @return 尚未初始化的程序。
     */
    private fun compile(): PlannersScriptProgram {
        val source = SourceUnit("action", """
            import "@planners/bootstrap"
            import java com.novalang.runtime.ScriptCallbacks
            import java com.novalang.runtime.ScriptCallback
            import java com.novalang.runtime.NovaTask
            import java java.lang.Thread
            fun execute(): Int { return Counter.next() }
            fun read() { return this.player }
            fun callback(): ScriptCallback { return ScriptCallbacks.bind(::read) }
            fun later(): NovaTask { return schedule(10L) { Counter.next() } }
            fun thread(): String { return Thread.currentThread().getName() }
        """.trimIndent(), root.resolve("skill.yml"), "action", 1, 0, null)
        return PlannersScriptProgram.compile(root.resolve("nova.config.yml"), listOf(source)) { nova ->
            nova.setScriptClassLoader(PlannersScriptProgramTest::class.java.classLoader)
        }
    }

    /**
     * 创建当前动作的函数标识。
     *
     * @param name 函数名。
     * @return 模块函数标识。
     */
    private fun unit(name: String): NovaScriptUnit {
        return NovaScriptUnit("action", name)
    }

    /**
     * 记录真实 Nova 调度资源的合成宿主。
     */
    private class CapturingScheduler : NovaScheduler {
        val tasks = ArrayList<CapturedTask>()
        var mainThread: Thread? = Thread.currentThread()
        val executor = Executors.newSingleThreadExecutor { command -> Thread(command, "planners-host-main") }

        /**
         * @return 当前调用是否位于模拟宿主主线程。
         */
        override fun isMainThread(): Boolean {
            return Thread.currentThread() === mainThread || Thread.currentThread().name == "planners-host-main"
        }

        /**
         * @return 模拟宿主主线程执行器。
         */
        override fun mainExecutor(): Executor {
            return executor
        }

        /**
         * 捕获定时任务，不推进真实时间。
         *
         * @param delayMs 延迟毫秒。
         * @param task Nova 实际生成的任务。
         * @return 可取消句柄。
         */
        override fun scheduleLater(delayMs: Long, task: Runnable): NovaScheduler.Cancellable {
            val captured = CapturedTask(task)
            tasks.add(captured)
            return captured
        }

        /**
         * 本测试不允许重复任务。
         *
         * @param initialDelayMs 首次延迟。
         * @param periodMs 重复周期。
         * @param task 调度任务。
         * @throws AssertionError 调度重复任务时抛出。
         */
        override fun scheduleRepeat(initialDelayMs: Long, periodMs: Long, task: Runnable): NovaScheduler.Cancellable {
            throw AssertionError("Unexpected repeating task")
        }
    }

    /**
     * 可在取消后模拟迟到触发的任务句柄。
     *
     * @param action 真实 Nova 调度回调。
     */
    private class CapturedTask(val action: Runnable) : NovaScheduler.Cancellable {
        var cancelled = false

        /**
         * 记录宿主收到的取消请求。
         */
        override fun cancel() {
            cancelled = true
        }

        /**
         * @return 是否已收到取消请求。
         */
        override fun isCancelled(): Boolean {
            return cancelled
        }
    }
}
