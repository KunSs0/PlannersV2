package com.gitee.planners.module.action.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证「字面量 / 路径 / 公式 / 模板」的自动判定。
 *
 * 判定顺序对齐 Creator 的 `QueryRef.parse`：数字、布尔、纯路径先命中，
 * 其余能解析成公式的按公式，解析不成的回落文本。
 */
class QueryRefDetectionTest {

    /** 构造带技能等级的上下文。 */
    private fun scope(): QueryScope {
        return QueryScope(
            skill = Any(),
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

    /** 数字按字面量，且保留整数形态。 */
    @Test
    fun detectsNumberLiteral() {
        val ref = QueryRef.parse("100", "test")
        assertTrue(ref.isLiteral())
        assertEquals(100L, ref.resolve(scope()))
    }

    /** 小数按字面量。 */
    @Test
    fun detectsDecimalLiteral() {
        val ref = QueryRef.parse("2.5", "test")
        assertTrue(ref.isLiteral())
        assertEquals(2.5, ref.resolve(scope()))
    }

    /** 布尔按字面量。 */
    @Test
    fun detectsBooleanLiteral() {
        val ref = QueryRef.parse("true", "test")
        assertTrue(ref.isLiteral())
        assertEquals(true, ref.resolve(scope()))
    }

    /** 纯路径按路径。 */
    @Test
    fun detectsPlainPath() {
        val ref = QueryRef.parse("skill.level", "test")
        assertFalse(ref.isLiteral())
        assertEquals(3L, ref.resolve(scope()))
    }

    /** 不带外壳的公式按公式求值。 */
    @Test
    fun detectsBareFormula() {
        val ref = QueryRef.parse("10 * skill.level + 50", "test")
        assertFalse(ref.isLiteral())
        assertEquals(80.0, ref.resolve(scope()))
    }

    /** 普通文本仍按文本。 */
    @Test
    fun detectsPlainText() {
        val ref = QueryRef.parse("&a冲锋", "test")
        assertTrue(ref.isLiteral())
        assertEquals("&a冲锋", ref.resolve(scope()))
    }

    /** 单个裸词按文本，不当成未知路径。 */
    @Test
    fun detectsSingleWordAsText() {
        val ref = QueryRef.parse("hello", "test")
        assertTrue(ref.isLiteral())
        assertEquals("hello", ref.resolve(scope()))
    }

    /** 尖括号包裹按取值，不是模板。 */
    @Test
    fun detectsWrappedValue() {
        val ref = QueryRef.parse("<skill.level>", "test")
        assertEquals(3L, ref.resolve(scope()))
    }

    /** 文本内嵌占位符按模板。 */
    @Test
    fun detectsTemplate() {
        val ref = QueryRef.parse("Lv<skill.level>", "test")
        assertFalse(ref.isLiteral())
        assertEquals("Lv3", ref.resolveText(scope()))
    }

    /** 带引号的文本去掉引号后按文本。 */
    @Test
    fun detectsQuotedText() {
        val ref = QueryRef.parse("\"hurt1\"", "test")
        assertEquals("hurt1", ref.resolveText(scope()))
    }
}
