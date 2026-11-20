package com.gitee.planners.core.skill.pick

import com.gitee.planners.api.pick.EntityPickDefinition
import com.gitee.planners.api.pick.PickDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证 `__option__.pick.fallback` 的解析与透传。
 *
 * 兜底策略决定“没有选中任何合规实体”时技能是否继续，配置写错必须直接报错，
 * 不能静默当作未声明。
 */
class EntityPickFallbackTest {

    /** 声明 `self` 时必须解析成自救，并随字段表下发给表现层。 */
    @Test
    fun parsesSelfFallback() {
        val definition = parse(
            """
            type: entity
            max-duration-ticks: 20
            range: 20
            max-angle-degrees: 35
            require-line-of-sight: true
            fallback: self
            """,
        )
        val entityDefinition = definition as EntityPickDefinition
        assertEquals(EntityPickDefinition.FALLBACK_SELF, entityDefinition.fallback)
        assertEquals(EntityPickDefinition.FALLBACK_SELF, entityDefinition.fields()["fallback"])
    }

    /** 未声明兜底时保持 null，指向失败仍按原语义派发失败反馈。 */
    @Test
    fun keepsNullWhenAbsent() {
        val definition = parse(
            """
            type: entity
            max-duration-ticks: 20
            range: 20
            max-angle-degrees: 35
            require-line-of-sight: true
            """,
        )
        val entityDefinition = definition as EntityPickDefinition
        assertNull(entityDefinition.fallback)
        assertNull(entityDefinition.fields()["fallback"])
    }

    /** 未内置的兜底策略必须报错，避免配置写了却被忽略。 */
    @Test
    fun rejectsUnknownFallback() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            parse(
                """
                type: entity
                max-duration-ticks: 20
                range: 20
                max-angle-degrees: 35
                require-line-of-sight: true
                fallback: nearest-ally
                """,
            )
        }
        val message = error.message
        if (message == null) {
            throw AssertionError("expected a message")
        }
        if (!message.contains("fallback")) {
            throw AssertionError("unexpected message: $message")
        }
    }

    /**
     * 解析一段 `pick` 配置。
     *
     * @param yaml `pick` 段正文。
     * @return 解析后的定义。
     */
    private fun parse(yaml: String): PickDefinition {
        val builder = StringBuilder("pick:\n")
        for (line in yaml.trimIndent().lines()) {
            builder.append("  ")
            builder.append(line)
            builder.append('\n')
        }
        val configuration = Configuration.loadFromString(builder.toString())
        val section = configuration.getConfigurationSection("pick")
        if (section == null) {
            throw AssertionError("pick section missing")
        }
        return PickDefinitionParser.parse(PickSection(section), "test")
    }
}
