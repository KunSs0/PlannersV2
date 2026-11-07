package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证定时触发器与加载期流程。
 *
 * 使用手动调度器推进时间，不依赖 Bukkit 调度器。
 */
class ActionTimerServiceTest {

    private lateinit var dispatcher: ManualDispatcher

    /** 每个用例前安装手动调度器并恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(TickAction)
        TICKS.clear()
        dispatcher = ManualDispatcher()
        ActionScheduler.install(dispatcher)
    }

    /** 每个用例后卸载调度器并停止定时器。 */
    @AfterEach
    fun cleanup() {
        ActionTimerService.stop()
        ActionScheduler.uninstall()
    }

    /** 读取一段 YAML 中的 `action` 节点。 */
    private fun section(yaml: String): taboolib.library.configuration.ConfigurationSection {
        val builder = StringBuilder()
        builder.append("action:\n")
        for (line in yaml.trimEnd().lines()) {
            if (line.isEmpty()) {
                builder.append('\n')
                continue
            }
            builder.append("  ")
            builder.append(line)
            builder.append('\n')
        }
        val configuration = Configuration.loadFromString(builder.toString())
        val result = configuration.getConfigurationSection("action")
        if (result == null) {
            throw IllegalStateException("action section missing")
        }
        return result
    }

    /** 构建单技能结构。 */
    private fun spec(id: String, yaml: String): ActionSpec {
        val parsed = ActionParser.parse(id, section(yaml + "\n"), "action")
        if (parsed == null) {
            throw IllegalStateException("spec must not be null")
        }
        return parsed
    }

    /** 定时器行在重建时注册为周期任务。 */
    @Test
    fun registersRepeatingTask() {
        val skill = spec("s1", "fire:\n- 'tick{tag=\"a\"} ~onTimer{ticks=20}'")
        ActionTimerService.restart(listOf(skill))
        assertEquals(1, dispatcher.tasks.size)
        assertEquals(20L, dispatcher.tasks[0].periodTicks)
        assertEquals(0L, dispatcher.tasks[0].delayTicks)
    }

    /** 到点后执行动作。 */
    @Test
    fun runsActionOnTick() {
        val skill = spec("s1", "fire:\n- 'tick{tag=\"a\"} ~onTimer{ticks=20}'")
        ActionTimerService.restart(listOf(skill))
        dispatcher.tick()
        assertEquals(listOf("a"), TICKS)
    }

    /** 定时器行的流程条件成立时才执行。 */
    @Test
    fun skipsWhenFlowConditionFails() {
        val yaml = """
            fire:
              conditions:
                - 'chance{value=0}'
              actions:
                - 'tick{tag="a"} ~onTimer{ticks=20}'
        """.trimIndent()
        val skill = spec("s1", yaml)
        ActionTimerService.restart(listOf(skill))
        dispatcher.tick()
        assertTrue(TICKS.isEmpty())
    }

    /** times 限制执行次数。 */
    @Test
    fun respectsTimesLimit() {
        val skill = spec("s1", "fire:\n- 'tick{tag=\"a\"} ~onTimer{ticks=20; times=2}'")
        ActionTimerService.restart(listOf(skill))
        dispatcher.tick()
        dispatcher.tick()
        dispatcher.tick()
        assertEquals(2, TICKS.size)
    }

    /** stop 取消全部任务。 */
    @Test
    fun stopCancelsTasks() {
        val skill = spec("s1", "fire:\n- 'tick{tag=\"a\"} ~onTimer{ticks=20}'")
        ActionTimerService.restart(listOf(skill))
        ActionTimerService.stop()
        assertTrue(dispatcher.tasks[0].cancelled)
        dispatcher.tick()
        assertTrue(TICKS.isEmpty())
    }

    /** 重建会取消上一代任务，不产生重复定时器。 */
    @Test
    fun restartReplacesPreviousTasks() {
        val skill = spec("s1", "fire:\n- 'tick{tag=\"a\"} ~onTimer{ticks=20}'")
        ActionTimerService.restart(listOf(skill))
        ActionTimerService.restart(listOf(skill))
        assertEquals(2, dispatcher.tasks.size)
        assertTrue(dispatcher.tasks[0].cancelled)
        assertFalse(dispatcher.tasks[1].cancelled)
    }

    /** 周期必须是字面量，动态周期在加载期报错。 */
    @Test
    fun rejectsDynamicPeriod() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("tick{tag=\"a\"} ~onTimer{ticks=<skill.level>}", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("literal"))
    }

    /** 非定时触发器不注册周期任务。 */
    @Test
    fun ignoresEventTriggerLines() {
        val skill = spec("s1", "server:\n- 'tick{tag=\"a\"} ~on_quit'")
        ActionTimerService.restart(listOf(skill))
        assertTrue(dispatcher.tasks.isEmpty())
    }

    /** server 流程在加载期执行一次。 */
    @Test
    fun runsServerFlowOnce() {
        val skill = spec("s1", "server:\n- 'tick{tag=\"boot\"}'")
        val executed = ActionServerFlows.runAll(listOf(skill))
        assertEquals(1, executed)
        assertEquals(listOf("boot"), TICKS)
    }

    /** server 流程不处理定时器行。 */
    @Test
    fun serverFlowSkipsTimerLines() {
        val skill = spec("s1", "server:\n- 'tick{tag=\"boot\"} ~onTimer{ticks=20}'")
        ActionServerFlows.runAll(listOf(skill))
        assertTrue(TICKS.isEmpty())
    }

    /**
     * 记录调用参数的测试动作。
     */
    private object TickAction : Action {

        override val id = "tick"

        override val aliases = listOf("tick")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("tag")
            arguments.rejectUnknown("tag")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            TICKS.add(arguments.requireText(scope, "tag"))
        }
    }

    private companion object {

        /** 记录动作执行顺序。 */
        val TICKS = ArrayList<String>()
    }
}
