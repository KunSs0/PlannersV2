package com.gitee.planners.module.action.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 验证列表下标取值。
 */
class QueryIndexTest {

    /** 构造带数组变量与技能等级的上下文。 */
    private fun scope(): QueryScope {
        return QueryScope(
            skill = Any(),
            variables = mapOf(
                "table" to listOf(12, 30, 50),
                "matrix" to listOf(listOf(1, 2), listOf(3, 4)),
            ),
            skillFieldReader = { _, field ->
                if (field == "level") {
                    2L
                } else {
                    null
                }
            },
            label = "test",
        )
    }

    /** 常量下标取出对应元素。 */
    @Test
    fun readsByConstantIndex() {
        val ref = QueryRef.parseExpression("skill.var.table[0]", "test")
        assertEquals(12, ref.resolve(scope()))
    }

    /** 下标可以是表达式。 */
    @Test
    fun readsByComputedIndex() {
        val ref = QueryRef.parseExpression("skill.var.table[skill.level - 1]", "test")
        assertEquals(30, ref.resolve(scope()))
    }

    /** 数组值直接走变量引用。 */
    @Test
    fun readsWholeList() {
        val ref = QueryRef.parseExpression("skill.var.table", "test")
        assertEquals(listOf(12, 30, 50), ref.resolve(scope()))
    }

    /** 越界返回 null 而不是抛异常。 */
    @Test
    fun outOfRangeReturnsNull() {
        val ref = QueryRef.parseExpression("skill.var.table[9]", "test")
        assertNull(ref.resolve(scope()))
    }

    /** 负下标返回 null。 */
    @Test
    fun negativeIndexReturnsNull() {
        val ref = QueryRef.parseExpression("skill.var.table[0 - 1]", "test")
        assertNull(ref.resolve(scope()))
    }

    /** 非列表目标返回 null。 */
    @Test
    fun nonListTargetReturnsNull() {
        val ref = QueryRef.parseExpression("skill.level[0]", "test")
        assertNull(ref.resolve(scope()))
    }

    /** 支持连续下标。 */
    @Test
    fun readsNestedIndex() {
        val ref = QueryRef.parseExpression("skill.var.matrix[1][0]", "test")
        assertEquals(3, ref.resolve(scope()))
    }

    /** 下标参与算术运算。 */
    @Test
    fun indexParticipatesInArithmetic() {
        val ref = QueryRef.parseExpression("skill.var.table[skill.level - 1] * 2", "test")
        assertEquals(60.0, ref.resolve(scope()))
    }

    /** 缺右括号时报错。 */
    @Test
    fun rejectsMissingBracket() {
        val failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            QueryRef.parseExpression("skill.var.table[0", "test")
        }
        assertEquals(true, failure.message!!.contains("expects ']'"))
    }
}
