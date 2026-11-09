package com.gitee.planners.module.action.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证带占位符的文本模板。
 */
class QueryTemplateTest {

    /** 构造带技能等级与名字的上下文。 */
    private fun scope(): QueryScope {
        return QueryScope(
            skill = Any(),
            variables = mapOf("speed" to 95.0),
            skillFieldReader = { _, field ->
                when (field) {
                    "level" -> 3L
                    "name" -> "冲锋"
                    else -> null
                }
            },
            label = "test",
        )
    }

    /** 单占位符按取值渲染。 */
    @Test
    fun rendersSinglePlaceholder() {
        val ref = QueryRef.parse("&a冲锋 Lv<skill.level>", "test.m")
        assertEquals("&a冲锋 Lv3", ref.resolveText(scope()))
    }

    /** 多个占位符与纯文本混排。 */
    @Test
    fun rendersMultiplePlaceholders() {
        val ref = QueryRef.parse("<skill.name> 速度 <skill.var.speed>%", "test.m")
        assertEquals("冲锋 速度 95%", ref.resolveText(scope()))
    }

    /** 占位符内可以是表达式。 */
    @Test
    fun rendersExpressionPlaceholder() {
        val ref = QueryRef.parse("加成 <skill.level * 15 + 80>", "test.m")
        assertEquals("加成 125", ref.resolveText(scope()))
    }

    /** 整段被尖括号包裹时仍按取值而非模板处理。 */
    @Test
    fun wholeWrappedValueIsNotTemplate() {
        val ref = QueryRef.parse("<skill.level>", "test.m")
        assertEquals(3L, ref.resolve(scope()))
    }

    /** 文案里的比较符号保持原样。 */
    @Test
    fun keepsComparisonSymbols() {
        val ref = QueryRef.parse("要求 a < b 且 c > d", "test.m")
        assertEquals("要求 a < b 且 c > d", ref.resolveText(scope()))
    }

    /** 未知根的占位符按普通文本处理。 */
    @Test
    fun unknownRootStaysLiteral() {
        val ref = QueryRef.parse("值 <nope.x>", "test.m")
        assertEquals("值 <nope.x>", ref.resolveText(scope()))
    }

    /** 模板判定成立且非字面量。 */
    @Test
    fun templateIsNotLiteral() {
        val ref = QueryRef.parse("Lv<skill.level>", "test.m")
        assertFalse(ref.isLiteral())
    }

    /** 含占位符的文本被识别为模板候选。 */
    @Test
    fun detectsTemplateCandidate() {
        assertTrue(QueryTemplate.looksLikeTemplate("Lv<skill.level>"))
        assertFalse(QueryTemplate.looksLikeTemplate("no placeholder"))
        assertFalse(QueryTemplate.looksLikeTemplate("only < open"))
    }

    /** 缺失取值渲染为空串。 */
    @Test
    fun missingValueRendersEmpty() {
        val ref = QueryRef.parse("职业:<profile.job>", "test.m")
        assertEquals("职业:", ref.resolveText(scope()))
    }
}
