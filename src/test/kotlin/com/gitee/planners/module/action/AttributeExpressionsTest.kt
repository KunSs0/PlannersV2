package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证 `hook.attributes` 的表达式编译与求值。
 *
 * 使用与业务侧职业配置同构的合成 fixture，不读取服务端生产目录。
 */
class AttributeExpressionsTest {

    /** 构造带职业等级的上下文。 */
    private fun scope(level: Int): QueryScope {
        return QueryScope(
            profile = Any(),
            profileFieldReader = { _, field ->
                if (field == "level") {
                    level.toLong()
                } else {
                    null
                }
            },
            label = "job-attribute",
        )
    }

    /** 构造 `hook.attributes` 节点。 */
    private fun section(yaml: String): taboolib.library.configuration.ConfigurationSection {
        val builder = StringBuilder()
        for (line in yaml.trimEnd().lines()) {
            builder.append(line)
            builder.append('\n')
        }
        val configuration = Configuration.loadFromString(builder.toString())
        val result = configuration.getConfigurationSection("root")
        if (result == null) {
            throw IllegalStateException("root section missing")
        }
        return result
    }

    /** 数字常量直接作为属性值。 */
    @Test
    fun numericValueCompiles() {
        val expressions = AttributeExpressions.compileSection(
            section("root:\n  hook:\n    attributes:\n      FLAT: 7"),
            "job:test",
        )
        assertEquals(7.0, AttributeExpressions.evaluate(expressions["FLAT"]!!, scope(1)))
    }

    /** 线性公式按职业等级求值。 */
    @Test
    fun linearFormulaEvaluates() {
        val expressions = AttributeExpressions.compileSection(
            section("root:\n  hook:\n    attributes:\n      STR: \"10 + (profile.level - 1) * 1\""),
            "job:test",
        )
        assertEquals(10.0, AttributeExpressions.evaluate(expressions["STR"]!!, scope(1)))
        assertEquals(13.0, AttributeExpressions.evaluate(expressions["STR"]!!, scope(4)))
    }

    /** 取整改写与原 `round` 语义一致。 */
    @Test
    fun roundRewriteMatchesMathRound() {
        val expressions = AttributeExpressions.compileSection(
            section(
                "root:\n  hook:\n    attributes:\n" +
                    "      HP: \"((22 + (profile.level - 1) * 4) + 0.5 - ((22 + (profile.level - 1) * 4) + 0.5) % 1) - 20\"",
            ),
            "job:test",
        )
        for (level in 1..30) {
            val expected = Math.round(22.0 + (level - 1) * 4).toDouble() - 20
            val actual = AttributeExpressions.evaluate(expressions["HP"]!!, scope(level))
            assertEquals(expected, actual, "level=$level")
        }
    }

    /** 缺失的属性返回 null，由调用方跳过。 */
    @Test
    fun missingPathYieldsNull() {
        val expressions = AttributeExpressions.compileSection(
            section("root:\n  hook:\n    attributes:\n      X: \"profile.jobLevel\""),
            "job:test",
        )
        assertNull(AttributeExpressions.evaluate(expressions["X"]!!, scope(3)))
    }

    /** 未配置时返回空映射。 */
    @Test
    fun missingSectionYieldsEmpty() {
        val expressions = AttributeExpressions.compileSection(section("root:\n  name: x"), "job:test")
        assertEquals(0, expressions.size)
    }

    /** 配置类型非法时报错。 */
    @Test
    fun rejectsUnsupportedType() {
        assertThrows(IllegalArgumentException::class.java) {
            AttributeExpressions.compile(mapOf("a" to 1), "job:test.hook.attributes.X")
        }
    }
}
