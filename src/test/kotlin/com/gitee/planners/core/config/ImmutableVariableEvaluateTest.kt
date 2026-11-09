package com.gitee.planners.core.config

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * 验证技能变量的结构化求值。
 *
 * 覆盖常量、显式表达式、条件分支，以及「旧脚本表达式不在构造期解析」这一约束。
 */
class ImmutableVariableEvaluateTest {

    /** 构造带技能等级的上下文。 */
    private fun scope(variables: MutableMap<String, Any?> = LinkedHashMap()): QueryScope {
        return QueryScope(
            skill = Any(),
            variables = variables,
            skillFieldReader = { _, field ->
                if (field == "level") {
                    3L
                } else {
                    null
                }
            },
            label = "test",
        )
    }

    /** 字符串常量原样返回，保留引号与插值符号。 */
    @Test
    fun literalReturnsRawValue() {
        val variable = ImmutableVariable.parse("label", "a \"b\" <c>")
        assertEquals("a \"b\" <c>", variable.evaluate(scope()))
    }

    /** 列表常量返回副本，避免调用方改到配置对象。 */
    @Test
    fun literalListReturnsCopy() {
        val source = arrayListOf<Any>(1, 2, 3)
        val variable = ImmutableVariable.parse("table", source)
        val evaluated = variable.evaluate(scope())
        assertEquals(source, evaluated)
        if (evaluated is MutableList<*>) {
            evaluated.clear()
        }
        assertEquals(3, source.size)
    }

    /** 显式表达式交给查询引擎求值。 */
    @Test
    fun expressionUsesQueryEngine() {
        val variable = ImmutableVariable.parse("amount", mapOf("expression" to "skill.level + 1"))
        assertEquals(4.0, variable.evaluate(scope()))
    }

    /** 表达式可以引用此前已求值的变量。 */
    @Test
    fun expressionReadsEarlierVariables() {
        val accumulated = LinkedHashMap<String, Any?>()
        val first = ImmutableVariable.parse("attackLevel", 5)
        accumulated[first.id] = first.evaluate(scope(accumulated))
        val second = ImmutableVariable.parse("doubled", mapOf("expression" to "skill.var.attackLevel * 2"))
        assertEquals(10.0, second.evaluate(scope(accumulated)))
    }

    /** 条件变量命中时返回分支结果。 */
    @Test
    fun whenMatchesBranch() {
        val variable = ImmutableVariable.parse(
            "choice",
            listOf(
                mapOf("condition" to "skill.level > 2", "action" to "7"),
                mapOf("condition" to "skill.level > 0", "action" to "9"),
            ),
        )
        assertEquals(7.0, variable.evaluate(scope()))
    }

    /** 条件变量全部不成立时返回 false。 */
    @Test
    fun whenFallsBackToFalse() {
        val variable = ImmutableVariable.parse(
            "choice",
            listOf(
                mapOf("condition" to "skill.level > 100", "action" to "1"),
            ),
        )
        val result = variable.evaluate(scope())
        assertFalse(result as Boolean)
    }

    /** 旧脚本写法的表达式不在构造期解析，避免加载即失败。 */
    @Test
    fun legacyExpressionIsNotParsedAtConstruction() {
        val variable = ImmutableVariable.parse("attack_speed", mapOf("expression" to "level * 2"))
        assertEquals("attack_speed", variable.id)
    }

    /** 旧脚本写法的条件分支同样不在构造期解析。 */
    @Test
    fun legacyWhenIsNotParsedAtConstruction() {
        val variable = ImmutableVariable.parse(
            "choice",
            listOf(
                mapOf("condition" to "level > 1", "action" to "20"),
                mapOf("condition" to "true", "action" to "10"),
            ),
        )
        assertEquals("choice", variable.id)
    }
}
