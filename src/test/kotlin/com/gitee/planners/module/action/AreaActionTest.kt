package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID
import taboolib.module.configuration.Configuration

/**
 * 验证 `area.start` / `area.stop` 与回调作用域。
 *
 * 区域只提供几何与回调：回调里 `sender` 是区域主、`target` 是锚点、`area.self.*` 是本实例。
 */
class AreaActionTest {

    /** 记录回调现场。 */
    private val probe = Probe()

    /** 假调度器。 */
    private lateinit var dispatcher: FakeDispatcher

    /** 默认在线判定。 */
    private lateinit var defaultOwnerOnline: (UUID) -> Boolean

    /** 每个用例前重置状态。 */
    @BeforeEach
    fun setUp() {
        defaultOwnerOnline = AreaRegistry.ownerOnline
        AreaRegistry.ownerOnline = { true }
        AreaRegistry.clear()
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(ProbeAction(probe))
        dispatcher = FakeDispatcher()
        ActionScheduler.install(dispatcher)
    }

    /** 用例结束后恢复。 */
    @AfterEach
    fun tearDown() {
        ActionScheduler.uninstall()
        AreaRegistry.clear()
        AreaRegistry.ownerOnline = defaultOwnerOnline
    }

    /**
     * 构造区域主。
     *
     * @param location 区域主所在坐标。
     * @return 玩家替身。
     */
    private fun owner(location: Location): Player {
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.uniqueId).thenReturn(UUID.randomUUID())
        Mockito.`when`(player.location).thenReturn(location)
        Mockito.`when`(player.isOnline).thenReturn(true)
        Mockito.`when`(player.isValid).thenReturn(true)
        return player
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

    /** 启动区域会按 period 安排回调，回调能读到自身半径。 */
    @Test
    fun startSchedulesCallbackWithAreaContext() {
        val world = Mockito.mock(World::class.java)
        val castPoint = Location(world, 10.0, 64.0, 10.0)
        val player = owner(castPoint)
        val parsed = spec(
            """
            action:
              fire:
                - 'area.start{id="guard"; radius=4; ticks=100; period=10; on.run="tick"} @sender.loc'
              tick:
                - 'probe{}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(sender = player, caster = player, label = "area"))

        assertEquals(1, AreaRegistry.size())
        assertEquals(1, dispatcher.tasks.size)
        assertEquals(10L, dispatcher.tasks[0].periodTicks)

        dispatcher.tasks[0].runnable.run()

        assertEquals(1, probe.calls)
        assertEquals("guard", probe.id)
        assertEquals(4.0, probe.radius)
        assertEquals(castPoint, probe.target)
    }

    /** 没有目标器时锚点跟随施法者，区域随人移动。 */
    @Test
    fun anchorFallsBackToCaster() {
        val world = Mockito.mock(World::class.java)
        val castPoint = Location(world, 1.0, 64.0, 1.0)
        val player = owner(castPoint)
        val parsed = spec(
            """
            action:
              fire:
                - 'area.start{id="guard"; radius=2; ticks=100}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(sender = player, caster = player, label = "area"))
        assertEquals(1, AreaRegistry.count(castPoint, "guard"))
        assertEquals(0, dispatcher.tasks.size)
    }

    /** 停止只清自己的实例并取消回调。 */
    @Test
    fun stopCancelsOwnArea() {
        val world = Mockito.mock(World::class.java)
        val castPoint = Location(world, 0.0, 64.0, 0.0)
        val player = owner(castPoint)
        val parsed = spec(
            """
            action:
              fire:
                - 'area.start{id="guard"; radius=4; ticks=100; period=10; on.run="tick"} @sender.loc'
                - 'area.stop{id="guard"}'
              tick:
                - 'probe{}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(sender = player, caster = player, label = "area"))

        assertEquals(0, AreaRegistry.size())
        assertTrue(dispatcher.tasks[0].cancelled)
    }

    /** ticks 为 0 属于错误参数，加载期拒绝。 */
    @Test
    fun rejectsZeroTicks() {
        assertThrows(IllegalArgumentException::class.java) {
            spec(
                """
                action:
                  fire:
                    - 'area.start{id="guard"; radius=4; ticks=0}'
                """.trimIndent(),
            )
        }
    }

    /** period 与 on.run 必须成对出现。 */
    @Test
    fun rejectsUnpairedPeriod() {
        assertThrows(IllegalArgumentException::class.java) {
            spec(
                """
                action:
                  fire:
                    - 'area.start{id="guard"; radius=4; ticks=100; on.run="tick"}'
                  tick:
                    - 'probe{}'
                """.trimIndent(),
            )
        }
    }

    /** 回调现场的记录容器。 */
    private class Probe {

        var calls = 0

        var id: String? = null

        var radius: Double? = null

        var target: Any? = null
    }

    /**
     * 记录回调现场的测试动作。
     *
     * @property probe 所属用例的记录容器。
     */
    private class ProbeAction(private val probe: Probe) : Action {

        override val id = "probe"

        override val aliases = listOf("probe")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown()
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            probe.calls += 1
            probe.target = scope.target
            val idRef = QueryRef.parse("<area.self.id>", "probe")
            val radiusRef = QueryRef.parse("<area.self.radius>", "probe")
            probe.id = idRef.resolveText(scope)
            probe.radius = radiusRef.resolveDouble(scope)
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
