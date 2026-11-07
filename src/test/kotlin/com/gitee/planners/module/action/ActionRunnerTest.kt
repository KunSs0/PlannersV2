package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证动作执行器的条件门禁、触发器跳过与派发、目标器顺序与流程调用。
 *
 * 使用内置动作与一个只记录调用的测试动作，不依赖 Bukkit 运行环境。
 */
class ActionRunnerTest {

    /** 每个用例前恢复内置条目并注册测试动作。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(RecordAction)
        ActionRegistry.register(StrictRecordAction)
        ActionRegistry.register(DamageTrigger)
        ActionRegistry.register(QuitTrigger)
        RECORDS.clear()
        STRICT_RECORDS.clear()
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
        assertNotNull(result, "action section must be present")
        return result!!
    }

    /** 构造一个只带标签的作用域。 */
    private fun scope(): QueryScope {
        return QueryScope(label = "test")
    }

    /** 无条件流程按声明顺序执行全部动作。 */
    @Test
    fun runsAllLinesInOrder() {
        val spec = ActionParser.parse(
            "test",
            section("fire:\n- 'record{tag=\"a\"}'\n- 'record{tag=\"b\"}'\n"),
            "action",
        )
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        assertEquals(listOf("a", "b"), RECORDS)
    }

    /** 条件不成立时整个流程不执行。 */
    @Test
    fun skipsFlowWhenConditionFails() {
        val yaml = """
            fire:
              conditions:
                - 'chance{value=0}'
              actions:
                - 'record{tag="a"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        assertTrue(RECORDS.isEmpty())
    }

    /** 条件成立时流程正常执行。 */
    @Test
    fun runsFlowWhenConditionPasses() {
        val yaml = """
            fire:
              conditions:
                - 'chance{value=1}'
              actions:
                - 'record{tag="a"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        assertEquals(listOf("a"), RECORDS)
    }

    /** 带触发器的行在普通流程中被跳过。 */
    @Test
    fun skipsTriggeredLineDuringFlow() {
        val yaml = """
            fire:
              - 'record{tag="now"}'
              - 'record{tag="later"} ~onTimer{ticks=20}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        assertEquals(listOf("now"), RECORDS)
    }

    /** 事件派发时只执行触发器匹配的行。 */
    @Test
    fun firesMatchingTriggerOnly() {
        val yaml = """
            server:
              - 'record{tag="on-damage"} ~fc.on_damage_modify'
              - 'record{tag="on-quit"} ~on_quit'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        val executed = ActionRunner.fire(spec!!, "fc.on_damage_modify", scope())
        assertEquals(1, executed)
        assertEquals(listOf("on-damage"), RECORDS)
    }

    /** 定时触发器不参与事件派发。 */
    @Test
    fun timerLineIsNotFired() {
        val yaml = """
            fire:
              - 'record{tag="tick"} ~onTimer{ticks=20}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        val executed = ActionRunner.fire(spec!!, "onTimer", scope())
        assertEquals(0, executed)
        assertTrue(RECORDS.isEmpty())
    }

    /** call 执行另一个流程。 */
    @Test
    fun callsAnotherFlow() {
        val yaml = """
            fire:
              - 'call{list=inner}'
            inner:
              - 'record{tag="inner"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        assertEquals(listOf("inner"), RECORDS)
    }

    /** 循环引用在深度上限处报错。 */
    @Test
    fun rejectsCyclicCall() {
        val yaml = """
            fire:
              - 'call{list=fire}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        assertThrows(IllegalStateException::class.java) {
            ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        }
    }

    /** 引用不存在的流程直接报错。 */
    @Test
    fun rejectsUnknownFlow() {
        val yaml = """
            fire:
              - 'call{list=missing}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        }
        assertTrue(failure.message!!.contains("Unknown action flow"))
    }

    /** 目标器解析为空时动作被跳过。 */
    @Test
    fun skipsWhenTargetsEmpty() {
        val yaml = """
            fire:
              - 'strict-record{tag="a"} @limit{n=0}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        assertTrue(STRICT_RECORDS.isEmpty())
    }

    /** 目标器参数为动态值时按运行期值截断。 */
    @Test
    fun appliesLimitWithDynamicValue() {
        val yaml = """
            fire:
              - 'strict-record{tag="a"} @limit{n=<0>}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope())
        assertTrue(STRICT_RECORDS.isEmpty())
    }

    /** 空流程的条件不会阻止其他流程执行。 */
    @Test
    fun fireSkipsFlowWithFailingCondition() {
        val yaml = """
            server:
              conditions:
                - 'chance{value=0}'
              actions:
                - 'record{tag="a"} ~fc.on_damage_modify'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        val executed = ActionRunner.fire(spec!!, "fc.on_damage_modify", scope())
        assertEquals(0, executed)
        assertFalse(RECORDS.contains("a"))
    }

    /**
     * 只记录调用参数的测试动作，接受空目标。
     */
    private object RecordAction : Action {

        override val id = "record"

        override val aliases = listOf("record")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("tag")
            arguments.rejectUnknown("tag")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            RECORDS.add(arguments.requireText(scope, "tag"))
        }
    }

    /**
     * 不接受空目标的测试动作，用于验证空容器跳过语义。
     */
    private object StrictRecordAction : Action {

        override val id = "strict-record"

        override val aliases = listOf("strict-record")

        override fun validate(arguments: ActionArguments) {
            arguments.require("tag")
            arguments.rejectUnknown("tag")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            STRICT_RECORDS.add(arguments.requireText(scope, "tag"))
        }
    }

    /**
     * 测试用事件触发器：仅用于验证派发匹配，不连接真实事件。
     */
    private object DamageTrigger : ActionTrigger {

        override val id = "fc.on_damage_modify"

        override val aliases = listOf("fc.on_damage_modify")
    }

    /**
     * 测试用事件触发器：仅用于验证派发匹配，不连接真实事件。
     */
    private object QuitTrigger : ActionTrigger {

        override val id = "on_quit"

        override val aliases = listOf("on_quit")
    }

    private companion object {

        /** 记录动作的调用顺序。 */
        val RECORDS = ArrayList<String>()

        /** 记录不接受空目标的动作调用。 */
        val STRICT_RECORDS = ArrayList<String>()
    }
}