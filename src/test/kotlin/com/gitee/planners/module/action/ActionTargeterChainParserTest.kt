package com.gitee.planners.module.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.gitee.planners.module.action.query.QueryScope

/**
 * 验证目标器链文本解析。
 *
 * 同一套解析同时服务动作行、指向过滤链与 `targeter` 条件，因此这里覆盖条目与整段文本两种入口。
 */
class ActionTargeterChainParserTest {

    /** 每个用例前恢复内置目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 按声明顺序解析条目，参数保持可求值。 */
    @Test
    fun parsesEntriesInOrder() {
        val steps = ActionTargeterChainParser.parseEntries(listOf("@alive", "@wounded{ratio=0.3}"), "test.filter")
        assertEquals(2, steps.size)
        assertEquals("alive", steps[0].targeter.id)
        assertEquals("wounded", steps[1].targeter.id)
        assertEquals(0.3, steps[1].arguments.requireNumber(QueryScope(label = "test"), "ratio"))
    }

    /** 整段文本按空白拆成多步。 */
    @Test
    fun parsesChainText() {
        val steps = ActionTargeterChainParser.parseText("@crosshair{range=20} @ally @alive", "test.chain")
        assertEquals(3, steps.size)
        assertEquals("crosshair", steps[0].targeter.id)
        assertEquals(20.0, steps[0].arguments.requireNumber(QueryScope(label = "test"), "range"))
        assertEquals("ally", steps[1].targeter.id)
        assertEquals("alive", steps[2].targeter.id)
    }

    /** 引号与花括号内的空白不参与拆分。 */
    @Test
    fun keepsQuotedArgumentsTogether() {
        val steps = ActionTargeterChainParser.parseText("@name{pattern=\"a b\"} @alive", "test.chain")
        assertEquals(2, steps.size)
        assertEquals("name", steps[0].targeter.id)
        assertEquals("alive", steps[1].targeter.id)
    }

    /** 未注册的目标器在加载期报错，并列出可用目标器。 */
    @Test
    fun rejectsUnknownTargeter() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseText("@nope", "test.chain")
        }
        val message = error.message
        if (message == null) {
            throw AssertionError("expected a message")
        }
        if (!message.contains("unknown targeter '@nope'")) {
            throw AssertionError("unexpected message: $message")
        }
    }

    /** 缺少 `@` 前缀的条目直接报错。 */
    @Test
    fun rejectsEntryWithoutMarker() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseText("alive", "test.chain")
        }
        val message = error.message
        if (message == null) {
            throw AssertionError("expected a message")
        }
        if (!message.contains("must start with '@'")) {
            throw AssertionError("unexpected message: $message")
        }
    }

    /** 目标器名之后只允许参数块，其他文本视为配置错误。 */
    @Test
    fun rejectsTrailingText() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseEntries(listOf("@alive extra"), "test.chain")
        }
        val message = error.message
        if (message == null) {
            throw AssertionError("expected a message")
        }
        if (!message.contains("unsupported text")) {
            throw AssertionError("unexpected message: $message")
        }
    }

    /** 缺失必填参数与多余参数都在加载期报错。 */
    @Test
    fun rejectsIllegalArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseEntries(listOf("@wounded{ratio=0.3; extra=1}"), "test.chain")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseEntries(listOf("@wounded{}"), "test.chain")
        }
    }

    /** 空文本、空条目与不配对的花括号都是配置错误。 */
    @Test
    fun rejectsMalformedText() {
        assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseText("   ", "test.chain")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseEntries(listOf("  "), "test.chain")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActionTargeterChainParser.parseText("@crosshair{range=20", "test.chain")
        }
        assertEquals(0, ActionTargeterChainParser.parseEntries(emptyList(), "test.chain").size)
    }
}
