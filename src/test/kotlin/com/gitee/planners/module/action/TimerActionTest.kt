package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import taboolib.module.configuration.Configuration

/**
 * 验证 `timer.start` / `timer.stop` 的作用域内行为。
 *
 * 定时任务只登记在当前执行的作用域里，按 id 分组；同一 id 可以有多份实例，
 * 停止时取消该分组的全部实例。
 */
class TimerActionTest {

    /** 本用例内的触发计数；每个测试方法新建实例，不跨用例共享。 */
    private val counter = Counter()

    /** 本用例内的假调度器。 */
    private lateinit var dispatcher: FakeDispatcher

    /** 每个用例前安装测试动作与假调度器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(CountingAction(counter))
        dispatcher = FakeDispatcher()
        ActionScheduler.install(dispatcher)
    }

    /** 用例结束后卸载调度器。 */
    @AfterEach
    fun tearDown() {
        ActionScheduler.uninstall()
    }

    /** 构造结构。 */
    private fun spec(yaml: String): ActionSpec {
        val configuration = Configuration.loadFromString(yaml)
        val parsed = ActionParser.parse("chain", configuration.getConfigurationSection("action"), "action")
        if (parsed == null) {
            throw IllegalStateException("spec must not be null")
        }
        return parsed
    }

    /** 启动后按周期触发流程，次数用尽自动取消。 */
    @Test
    fun startsRepeatingFlowAndStopsAfterTimes() {
        val parsed = spec(
            """
            action:
              fire:
                - 'timer.start{id="guard"; func=inner; every=10; times=2}'
              inner:
                - 'count{}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(label = "timer"))

        assertEquals(1, dispatcher.tasks.size)
        assertEquals(10L, dispatcher.tasks[0].periodTicks)
        val task = dispatcher.tasks[0]

        task.runnable.run()
        assertEquals(1, counter.steps)
        task.runnable.run()
        assertEquals(2, counter.steps)
        assertTrue(task.cancelled)

        task.runnable.run()
        assertEquals(2, counter.steps)
    }

    /** 同一 id 启动两次会得到两份实例，停止时全部取消。 */
    @Test
    fun sameIdKeepsBothInstances() {
        val parsed = spec(
            """
            action:
              fire:
                - 'timer.start{id="guard"; func=inner; every=10}'
                - 'timer.start{id="guard"; func=inner; every=10}'
                - 'timer.stop{id="guard"}'
              inner:
                - 'count{}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(label = "timer"))

        assertEquals(2, dispatcher.tasks.size)
        assertTrue(dispatcher.tasks[0].cancelled)
        assertTrue(dispatcher.tasks[1].cancelled)
    }

    /** 只在当前作用域生效：另一次执行拿不到上一次的句柄。 */
    @Test
    fun stopDoesNotReachOtherExecutions() {
        val parsed = spec(
            """
            action:
              start:
                - 'timer.start{id="guard"; func=inner; every=10}'
              inner:
                - 'count{}'
              stop:
                - 'timer.stop{id="guard"}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "start", QueryScope(label = "timer"))
        ActionRunner.run(parsed, "stop", QueryScope(label = "timer"))

        assertEquals(1, dispatcher.tasks.size)
        assertFalse(dispatcher.tasks[0].cancelled, "跨执行不应取消任务")
    }

    /** 字面量周期必须为正数，加载期即拒绝。 */
    @Test
    fun rejectsNonPositivePeriod() {
        assertThrows(IllegalArgumentException::class.java) {
            spec(
                """
                action:
                  fire:
                    - 'timer.start{id="guard"; func=inner; every=0}'
                  inner:
                    - 'count{}'
                """.trimIndent(),
            )
        }
    }

    /** 行上的 @this.loc 会被冻结，触发时子流程拿到的仍是施放坐标。 */
    @Test
    fun freezesLocationAtStart() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 8.0, 70.0, -2.0)
        val parsed = spec(
            """
            action:
              fire:
                - 'timer.start{id="zone"; func=inner; every=10; times=1} @this.loc'
              inner:
                - 'count{}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(sender = origin, label = "timer"))
        val task = dispatcher.tasks[0]
        task.runnable.run()

        assertEquals(1, counter.steps)
        assertEquals(origin, counter.lastTarget)
    }

    /** 触发计数容器，按用例持有。 */
    private class Counter {

        var steps = 0

        var lastTarget: Any? = null
    }

    /**
     * 记录触发次数的测试动作。
     *
     * @property counter 所属用例的计数器。
     */
    private class CountingAction(private val counter: Counter) : Action {

        override val id = "count"

        override val aliases = listOf("count")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown()
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            counter.steps += 1
            counter.lastTarget = scope.target
        }
    }

    /** 记录安排请求的假调度器。 */
    private class FakeDispatcher : ActionScheduler.Dispatcher {

        val tasks = ArrayList<FakeTask>()

        override fun schedule(delayTicks: Long, periodTicks: Long, task: Runnable): ActionTask {
            val created = FakeTask(task, delayTicks, periodTicks)
            tasks.add(created)
            return created
        }
    }

    /**
     * 可手动触发的假任务。
     *
     * @property runnable 任务体。
     * @property delayTicks 首次延迟。
     * @property periodTicks 重复周期。
     */
    private class FakeTask(
        val runnable: Runnable,
        val delayTicks: Long,
        val periodTicks: Long,
    ) : ActionTask {

        var cancelled = false

        override fun cancel() {
            cancelled = true
        }
    }
}
