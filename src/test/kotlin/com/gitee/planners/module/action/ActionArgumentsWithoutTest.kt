package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 验证参数子视图 [ActionArguments.without]。
 *
 * 转发型条目（例如过滤目标器把剩余参数交给规则）依赖该子视图：既要剔除自己的键，
 * 又要保留原始文本、配置路径与未知键校验能力。
 */
class ActionArgumentsWithoutTest {

    /** 剔除指定键后其余键与取值保持不变。 */
    @Test
    fun removesOnlyRequestedKeys() {
        val arguments = ActionArguments(mapOf("r" to "health.damaged", "max" to "1.0"), "skill.test.pulse[0]")
        val body = arguments.without("r")
        assertEquals(setOf("max"), body.keys)
        assertEquals(1.0, body.requireNumber(QueryScope(), "max"))
        assertFalse(body.contains("r"))
        assertEquals("skill.test.pulse[0]", body.path)
    }

    /** 子视图保留表达式原文，便于被引用的实现自己解析。 */
    @Test
    fun keepsExpressionSourceText() {
        val arguments = ActionArguments(
            mapOf("r" to "health.ratio", "e" to "target.health < target.max_health"),
            "skill.test.pulse[0]",
        )
        val body = arguments.without("r")
        assertEquals("target.health < target.max_health", body.raw("e"))
        assertTrue(body.isDynamic("e"))
    }

    /** 剔除不存在的键不改变参数表。 */
    @Test
    fun ignoresMissingKeys() {
        val arguments = ActionArguments(mapOf("r" to "health.damaged"), "skill.test")
        val body = arguments.without("negate")
        assertEquals(setOf("r"), body.keys)
    }

    /** 派生不会修改原始参数表。 */
    @Test
    fun keepsOriginalsUntouched() {
        val arguments = ActionArguments(mapOf("r" to "health.damaged", "max" to "1.0"), "skill.test")
        arguments.without("r", "max")
        assertEquals(setOf("r", "max"), arguments.keys)
    }

    /** 子视图沿用原路径做未知键报错。 */
    @Test
    fun reportsUnknownKeyWithSamePath() {
        val arguments = ActionArguments(mapOf("r" to "health.damaged", "max" to "1.0"), "skill.test.pulse[0]")
        val body = arguments.without("r")
        val error = assertThrows<IllegalArgumentException> {
            body.rejectUnknown()
        }
        assertEquals("skill.test.pulse[0] contains unsupported argument 'max'.", error.message)
    }
}
