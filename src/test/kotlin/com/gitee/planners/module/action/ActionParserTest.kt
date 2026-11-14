package com.gitee.planners.module.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证 `action` 节点的结构解析、行语法与加载期校验。
 */
class ActionParserTest {

    /** 每个用例前恢复内置条目，避免用例间互相影响。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /**
     * 把从第 0 列开始的文本缩进两格挂到 `action` 之下。
     */
    private fun actionYaml(yaml: String): String {
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
        return builder.toString()
    }

    /**
     * 读取一段 YAML 中的 `action` 节点。
     */
    private fun section(yaml: String): taboolib.library.configuration.ConfigurationSection {
        val configuration = Configuration.loadFromString(actionYaml(yaml))
        val result = configuration.getConfigurationSection("action")
        assertNotNull(result, "action section must be present")
        return result!!
    }

    /**
     * 读取一段 YAML 中的 `action` 原始值，用于列表与字符串形态。
     */
    private fun actionNode(yaml: String): Any? {
        val configuration = Configuration.loadFromString(actionYaml(yaml))
        return configuration.get("action")
    }

    /** 顶层行列表被解析为 fire 单流程。 */
    @Test
    fun parsesTopLevelLineListAsFire() {
        val spec = ActionParser.parse("test", actionNode("- 'log{text=\"a\"}'"), "action")
        assertNotNull(spec)
        val flow = spec!!.flow(ActionSpec.FIRE)
        assertNotNull(flow)
        assertEquals(1, flow!!.actions.size)
    }

    /** 顶层 conditions/actions 块等价于只有 fire 的单流程。 */
    @Test
    fun parsesTopLevelBlockAsFire() {
        val spec = ActionParser.parse(
            "test",
            section(
                """
                conditions:
                - 'expr{e="1 > 0"}'
                actions:
                - 'log{text="a"}'
                """.trimIndent(),
            ),
            "action",
        )
        assertNotNull(spec)
        val flow = spec!!.flow(ActionSpec.FIRE)
        assertNotNull(flow)
        assertEquals(1, flow!!.conditions.size)
        assertEquals(1, flow!!.actions.size)
    }

    /** 顶层字符串属于已移除的脚本写法，解析期直接拒绝。 */
    @Test
    fun rejectsTopLevelString() {
        assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parse("test", actionNode("'log{text=\"a\"}'"), "action")
        }
    }

    /** 纯列表流程被解析为无条件的动作行。 */
    @Test
    fun parsesListFlow() {
        val spec = ActionParser.parse("test", section("fire:\n- 'log{text=\"a\"}'\n- 'log{text=\"b\"}'\n"), "action")
        assertNotNull(spec)
        val flow = spec!!.flow(ActionSpec.FIRE)
        assertNotNull(flow)
        assertEquals(2, flow!!.actions.size)
        assertTrue(flow.conditions.isEmpty())
        assertEquals("log", flow.actions[0].action.id)
    }

    /** 条件段与动作段被分别解析。 */
    @Test
    fun parsesConditionsAndActions() {
        val yaml = """
              on-hit:
                conditions:
                  - 'expr{e="1 > 0"}'
                  - 'chance{value=1}'
                actions:
                  - 'log{text="hit"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        val flow = spec!!.flow("on-hit")!!
        assertEquals(2, flow.conditions.size)
        assertEquals(1, flow.actions.size)
        assertEquals("expr", flow.conditions[0].condition.id)
        assertTrue(flow.conditions[0].expected)
    }

    /** 末尾 false 表示取反。 */
    @Test
    fun parsesNegatedCondition() {
        val entry = ActionParser.parseCondition("expr{e=\"1 > 0\"} false", "action.on-hit.conditions[0]")
        assertTrue(!entry.expected)
    }

    /** 流程段中出现未知字段直接报错。 */
    @Test
    fun rejectsUnknownFlowField() {
        val yaml = """
              on-hit:
                trigger: 'x'
                actions:
                  - 'log{text="a"}'
        """.trimIndent()
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parse("test", section(yaml + "\n"), "action")
        }
        assertTrue(failure.message!!.contains("unsupported field"))
    }

    /** 未知动作名报错并列出可用动作。 */
    @Test
    fun rejectsUnknownAction() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("nope{text=\"a\"}", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("unknown action"))
    }

    /** 未知条件名报错。 */
    @Test
    fun rejectsUnknownCondition() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseCondition("nope{id=\"a\"}", "action.on-hit.conditions[0]")
        }
        assertTrue(failure.message!!.contains("unknown condition"))
    }

    /** 未知目标器报错。 */
    @Test
    fun rejectsUnknownTargeter() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("log{text=\"a\"} @nope", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("unknown targeter"))
    }

    /** 未知触发器报错。 */
    @Test
    fun rejectsUnknownTrigger() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("log{text=\"a\"} ~nope", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("unknown trigger"))
    }

    /** 一行只能有一个触发器。 */
    @Test
    fun rejectsMultipleTriggers() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine(
                "log{text=\"a\"} ~onTimer{ticks=20} ~onTimer{ticks=40}",
                "action.fire[0]",
            )
        }
        assertTrue(failure.message!!.contains("single"))
    }

    /** 触发器冒号写法等价于 ticks 参数。 */
    @Test
    fun parsesTriggerColonForm() {
        val line = ActionParser.parseLine("log{text=\"a\"} ~onTimer:20", "action.fire[0]")
        assertNotNull(line.trigger)
        assertEquals("onTimer", line.trigger!!.id)
        assertEquals(20, line.triggerArguments!!.literalInteger("ticks"))
    }

    /** 目标器与触发器可以同时出现，顺序为动作 → 目标器 → 触发器。 */
    @Test
    fun parsesTargeterAndTrigger() {
        val line = ActionParser.parseLine(
            "log{text=\"a\"} @self @limit{n=3} ~onTimer{ticks=20}",
            "action.fire[0]",
        )
        assertEquals(2, line.targeters.size)
        assertEquals("sender", line.targeters[0].targeter.id)
        assertEquals("limit", line.targeters[1].targeter.id)
        assertNotNull(line.trigger)
    }

    /** 触发器行的定时判定成立。 */
    @Test
    fun detectsTimerLine() {
        val line = ActionParser.parseLine("log{text=\"a\"} ~onTimer{ticks=20}", "action.fire[0]")
        assertTrue(line.hasTrigger())
        assertTrue(line.isTimer())
    }

    /** 没有任何动作时返回 null。 */
    @Test
    fun returnsNullForEmptySection() {
        val configuration = Configuration.fromMap(mapOf("action" to emptyMap<String, Any>()))
        val result = ActionParser.parse("test", configuration.getConfigurationSection("action"), "action")
        assertNull(result)
    }

    /** 参数支持引号包裹的分号与花括号。 */
    @Test
    fun parsesQuotedArguments() {
        val line = ActionParser.parseLine(
            "log{text=\"a;b}c\"}",
            "action.fire[0]",
        )
        assertEquals("log", line.action.id)
        assertTrue(line.arguments.keys.contains("text"))
    }

    /** 重复参数直接报错。 */
    @Test
    fun rejectsDuplicateArgument() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("log{text=\"a\"; text=\"b\"}", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("duplicate"))
    }

    /** 缺少必填参数在加载期报错。 */
    @Test
    fun rejectsMissingRequiredArgument() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("message{}", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("requires argument"))
    }

    /** 未支持的多余参数在加载期报错。 */
    @Test
    fun rejectsUnsupportedArgument() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("log{text=\"a\"; extra=\"b\"}", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("unsupported argument"))
    }
}
