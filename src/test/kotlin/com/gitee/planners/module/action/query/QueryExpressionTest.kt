package com.gitee.planners.module.action.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证求值表达式的词法、语法、优先级与路径取值。
 *
 * 测试不依赖 Bukkit 宿主对象，全部通过 [QueryScope] 的函数注入提供数据。
 */
class QueryExpressionTest {

    /** 提供一个只认识固定字段的作用域。 */
    private fun scope(): QueryScope {
        return QueryScope(
            skill = STUB_SKILL,
            variables = mapOf("power" to 100L, "label" to "测试"),
            skillFieldReader = { _, field ->
                when (field) {
                    "id" -> "knight_skill_1"
                    "level" -> 3L
                    else -> null
                }
            },
            dataReader = { key ->
                if (key == "knight.skill6.field") {
                    "field-value"
                } else {
                    null
                }
            },
            attrReader = { key ->
                if (key == "STR") {
                    42.0
                } else {
                    null
                }
            },
            naturalReader = { key ->
                if (key == "STR") {
                    10.0
                } else {
                    null
                }
            },
            nodeReader = { tree, node ->
                if (tree == "knight_warder" && node == "knight-F-01_a") {
                    1
                } else {
                    0
                }
            },
            label = "test",
        )
    }

    /** 纯路径直接读取变量。 */
    @Test
    fun readsVariableByPlainPath() {
        val ref = QueryRef.parse("<skill.power>", "test")
        assertEquals(100L, ref.resolve(scope()))
    }

    /** 路径可读取宿主注入的属性。 */
    @Test
    fun readsInjectedHostValues() {
        val strength = QueryRef.parse("<attr.STR>", "test").resolve(scope())
        val natural = QueryRef.parse("<natural.STR>", "test").resolve(scope())
        val nodeLevel = QueryRef.parse("<node.knight_warder.knight-F-01_a>", "test").resolve(scope())
        assertEquals(42.0, strength)
        assertEquals(10.0, natural)
        assertEquals(1, nodeLevel)
    }

    /** data 的点号不切分，整段作为 key。 */
    @Test
    fun keepsDataKeyUnsplit() {
        val value = QueryRef.parse("<data.knight.skill6.field>", "test").resolve(scope())
        assertEquals("field-value", value)
    }

    /** 算术与括号按优先级求值。 */
    @Test
    fun evaluatesArithmeticWithPrecedence() {
        val value = QueryRef.parse("<skill.level * 2 + 4>", "test").resolveDouble(scope())
        assertEquals(10.0, value)
        val grouped = QueryRef.parse("<(skill.level + 1) * 2>", "test").resolveDouble(scope())
        assertEquals(8.0, grouped)
    }

    /** 取模用于向下取整。 */
    @Test
    fun floorsWithModulo() {
        val value = QueryRef.parse("<7.8 - 7.8 % 1>", "test").resolveDouble(scope())
        assertEquals(7.0, value)
    }

    /** 四舍五入用加法与取模表达。 */
    @Test
    fun roundsHalfUpWithModulo() {
        val value = QueryRef.parse("<7.4 + 0.5 - (7.4 + 0.5) % 1>", "test").resolveDouble(scope())
        assertEquals(7.0, value)
        val upper = QueryRef.parse("<7.6 + 0.5 - (7.6 + 0.5) % 1>", "test").resolveDouble(scope())
        assertEquals(8.0, upper)
    }

    /** 比较与逻辑运算返回布尔。 */
    @Test
    fun evaluatesComparisonAndLogic() {
        val pass = QueryRef.parse("<skill.level >= 3 && attr.STR > 40>", "test").resolveBoolean(scope())
        assertTrue(pass)
        val fail = QueryRef.parse("<skill.level >= 5 || attr.STR > 100>", "test").resolveBoolean(scope())
        assertFalse(fail)
    }

    /** 三元表达式按条件选择分支。 */
    @Test
    fun evaluatesTernary() {
        val value = QueryRef.parse("<skill.level > 1 ? 10 : 20>", "test").resolveDouble(scope())
        assertEquals(10.0, value)
    }

    /** 逻辑非与负号为一元运算。 */
    @Test
    fun evaluatesUnaryOperators() {
        val negative = QueryRef.parse("<-skill.power>", "test").resolveDouble(scope())
        assertEquals(-100.0, negative)
        val notFlag = QueryRef.parse("<!(skill.level > 10)>", "test").resolveBoolean(scope())
        assertTrue(notFlag)
    }

    /** 纯字面量不进入表达式解析。 */
    @Test
    fun parsesLiteralWithoutExpression() {
        val ref = QueryRef.parse("100", "test")
        assertTrue(ref.isLiteral())
        assertEquals(100L, ref.resolve(scope()))
        val text = QueryRef.parse("hello", "test")
        assertTrue(text.isLiteral())
        assertEquals("hello", text.resolve(scope()))
    }

    /** 调用函数视为配置错误。 */
    @Test
    fun rejectsFunctionCalls() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            QueryRef.parse("<round(1.5)>", "test")
        }
        assertTrue(failure.message!!.contains("function calls"))
    }

    /** 未知根节点直接报错并列出可用根。 */
    @Test
    fun rejectsUnknownRoot() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            QueryRef.parse("<round.attack.curr + 1>", "test")
        }
        assertTrue(failure.message!!.contains("unknown identifier"))
    }

    /** node 路径必须写树与节点两段。 */
    @Test
    fun requiresNodeTreeAndId() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            QueryRef.parse("<node.knight-F-01_a>", "test")
        }
        assertTrue(failure.message!!.contains("must be"))
    }

    /** 除零在运行期报配置错误。 */
    @Test
    fun rejectsDivideByZero() {
        val ref = QueryRef.parse("<skill.power / 0>", "test")
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ref.resolveDouble(scope())
        }
        assertTrue(failure.message!!.contains("divides by zero"))
    }

    /** 整数结果渲染为不带小数位的文本。 */
    @Test
    fun rendersIntegralText() {
        val ref = QueryRef.parse("<skill.power / 4>", "test")
        assertEquals("25", ref.resolveText(scope()))
    }

    /** 减法两侧留空格时按运算符处理。 */
    @Test
    fun subtractsWithSpaces() {
        val value = QueryRef.parse("<skill.power - 40>", "test").resolveDouble(scope())
        assertEquals(60.0, value)
    }

    /** 引号字符串字面量去掉引号。 */
    @Test
    fun parsesQuotedString() {
        val value = QueryRef.parse("'knight_warder'", "test").resolve(scope())
        assertEquals("knight_warder", value)
    }

    private companion object {

        /** 技能字段读取器只按字段名回答，占位对象本身不被使用。 */
        val STUB_SKILL = Any()
    }
}
