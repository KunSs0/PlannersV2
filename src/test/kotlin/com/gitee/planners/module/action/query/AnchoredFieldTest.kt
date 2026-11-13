package com.gitee.planners.module.action.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 验证 `target.anchored` 计算字段。
 *
 * 该字段的默认读取器由 Planners 的 MythicMobs 仇恨读取提供；没有 MythicMobs
 * 运行环境时恒为 false。这里验证字段本身按上下文读取，以及可用于条件与三元表达式。
 */
class AnchoredFieldTest {

    /** 默认上下文没有仇恨读取时恒为 false。 */
    @Test
    fun defaultIsFalse() {
        val ref = QueryRef.parse("target.anchored", "test")
        assertEquals(false, ref.resolve(scope("mob", "player", null)))
    }

    /** 按上下文提供的判定结果返回。 */
    @Test
    fun usesContextReader() {
        val reader: (Any?, Any?) -> Boolean = { target, sender ->
            target == "mob" && sender == "player"
        }
        val ref = QueryRef.parse("target.anchored", "test")
        assertEquals(true, ref.resolve(scope("mob", "player", reader)))
        assertEquals(false, ref.resolve(scope("mob", "other", reader)))
    }

    /** 可用于条件表达式。 */
    @Test
    fun worksInConditionExpression() {
        val ref = QueryRef.parseExpression("target.anchored && skill.level > 1", "test")
        assertEquals(true, ref.resolveBoolean(scope("mob", "player") { _, _ -> true }))
    }

    /** 可用于三元表达式。 */
    @Test
    fun worksInTernary() {
        val ref = QueryRef.parseExpression("target.anchored ? 1.2 : 1.0", "test")
        assertEquals(1.0, ref.resolve(scope("mob", "player") { _, _ -> false }))
    }

    /** 构造带锚定读取器的上下文。 */
    private fun scope(
        target: Any?,
        sender: Any?,
        reader: ((Any?, Any?) -> Boolean)?,
    ): QueryScope {
        return QueryScope(
            sender = sender,
            target = target,
            skill = Any(),
            skillFieldReader = { _, field ->
                if (field == "level") {
                    3L
                } else {
                    null
                }
            },
            anchoredReader = reader ?: { _, _ -> false },
            label = "test",
        )
    }
}
