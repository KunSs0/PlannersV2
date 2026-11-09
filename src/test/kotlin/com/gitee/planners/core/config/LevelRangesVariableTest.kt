package com.gitee.planners.core.config

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 验证技能变量的等级分段写法。
 *
 * 覆盖单级、闭区间、表达式取值、缺档与重叠校验，以及旧 expression 单键写法的兼容。
 */
class LevelRangesVariableTest {

    /**
     * 构造按技能等级取值的上下文。
     *
     * @param level 当前技能等级。
     * @param variables 已声明的变量。
     * @return 查询上下文。
     */
    private fun scope(level: Int, variables: Map<String, Any?> = emptyMap()): QueryScope {
        return QueryScope(
            skill = Any(),
            variables = variables,
            skillFieldReader = { _, field ->
                if (field == "level") {
                    level
                } else {
                    null
                }
            },
            label = "test",
        )
    }

    /** 单级与闭区间按等级取值。 */
    @Test
    fun picksValueByLevel() {
        val variable = ImmutableVariable.parse(
            "cooldown",
            linkedMapOf("1" to 160, "2-3" to 140, "4-5" to 120),
            5,
        )

        assertEquals(160L, variable.evaluate(scope(1)))
        assertEquals(140L, variable.evaluate(scope(2)))
        assertEquals(140L, variable.evaluate(scope(3)))
        assertEquals(120L, variable.evaluate(scope(5)))
    }

    /** 等级 0 与预览取最低段。 */
    @Test
    fun levelZeroUsesLowestSpan() {
        val variable = ImmutableVariable.parse("power", linkedMapOf("1-2" to 50, "3-5" to 80), 5)

        assertEquals(50L, variable.evaluate(scope(0)))
    }

    /** 显式声明 0 级时优先使用该段。 */
    @Test
    fun explicitZeroSpanWins() {
        val variable = ImmutableVariable.parse("power", linkedMapOf("0" to 10, "1-5" to 50), 5)

        assertEquals(10L, variable.evaluate(scope(0)))
        assertEquals(50L, variable.evaluate(scope(1)))
    }

    /** 分段取值可以是表达式，直接写 level 与其它变量。 */
    @Test
    fun evaluatesExpressionInsideSpan() {
        val variable = ImmutableVariable.parse(
            "reduction",
            linkedMapOf("1-3" to "3 + level", "4-5" to "power * 2"),
            5,
        )

        assertEquals(5.0, variable.evaluate(scope(2)))
        assertEquals(20.0, variable.evaluate(scope(5, mapOf("power" to 10.0))))
    }

    /** 旧的 expression 单键写法继续可用。 */
    @Test
    fun keepsSingleExpressionForm() {
        val variable = ImmutableVariable.parse("castSeconds", linkedMapOf("expression" to "level / 10.0"), 5)

        assertEquals(0.3, variable.evaluate(scope(3)))
    }

    /** 缺档在加载期报错并列出等级。 */
    @Test
    fun rejectsMissingLevels() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ImmutableVariable.parse("power", linkedMapOf("1-2" to 50, "4-5" to 80), 5)
        }

        assertTrue(error.message!!.contains("3"))
    }

    /** 重叠分段在加载期报错。 */
    @Test
    fun rejectsOverlappingSpans() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ImmutableVariable.parse("power", linkedMapOf("1-3" to 50, "3-5" to 80), 5)
        }

        assertTrue(error.message!!.contains("多个分段"))
    }

    /** 区间倒置在加载期报错。 */
    @Test
    fun rejectsReversedSpan() {
        assertThrows(IllegalArgumentException::class.java) {
            ImmutableVariable.parse("power", linkedMapOf("4-2" to 50), 5)
        }
    }

    /** 非法键给出可用写法提示。 */
    @Test
    fun rejectsUnknownKey() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ImmutableVariable.parse("power", linkedMapOf("table" to listOf(1, 2)), 5)
        }

        assertTrue(error.message!!.contains("expression"))
    }

    /** 没有等级上限的配置不能使用分段。 */
    @Test
    fun rejectsSpansWithoutMaxLevel() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ImmutableVariable.parse("power", linkedMapOf("1" to 50), null)
        }

        assertTrue(error.message!!.contains("等级上限"))
    }

    /** 超出上限的等级在求值期报错，不静默取末段。 */
    @Test
    fun rejectsLevelAboveSpans() {
        val variable = ImmutableVariable.parse("power", linkedMapOf("1-5" to 50), 5)

        assertThrows(IllegalArgumentException::class.java) {
            variable.evaluate(scope(6))
        }
    }

    /** YAML 里未加引号的数字键也要能解析成分段。 */
    @Test
    fun loadsYamlNumericKeys() {
        val config = Configuration.loadFromFile(File("src/test/resources/level-ranges.yml"))
        val section = config.getConfigurationSection("skill.variables")
        assertTrue(section != null)
        val power = ImmutableVariable.parse("power", section!!.get("power")!!, 5)
        val reduction = ImmutableVariable.parse("reduction", section.get("reduction")!!, 5)

        assertEquals(50L, power.evaluate(scope(1)))
        assertEquals(70L, power.evaluate(scope(3)))
        assertEquals(100L, power.evaluate(scope(5)))
        assertEquals(6.0, reduction.evaluate(scope(3)))
    }
}
