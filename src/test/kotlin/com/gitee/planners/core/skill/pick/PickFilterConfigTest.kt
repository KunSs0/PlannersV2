package com.gitee.planners.core.skill.pick

import com.gitee.planners.module.action.ActionRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证 `pick.filter` 配置段的解析。
 *
 * 覆盖正常链、未配置、缺少子段与未知类别；只读内存中的 YAML，不依赖 Bukkit 运行环境。
 */
class PickFilterConfigTest {

    /** 每个用例前恢复内置目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 按声明顺序解析 `filter.entity.targeters`。 */
    @Test
    fun parsesEntityFilterChain() {
        val steps = PickFilterConfig.parse(
            pickSection(
                """
                type: fc.entity
                max-duration-ticks: 20
                filter:
                  entity:
                    range: 15.0
                    targeters:
                      - '@alive'
                      - '@ally'
                """,
            ),
            "test",
        )
        assertEquals(2, steps.size)
        assertEquals("alive", steps[0].targeter.id)
        assertEquals("ally", steps[1].targeter.id)
    }

    /** 未配置过滤链时保持空列表，表示只做几何校验。 */
    @Test
    fun keepsEmptyChainWhenAbsent() {
        val steps = PickFilterConfig.parse(
            pickSection(
                """
                type: fc.entity
                max-duration-ticks: 20
                """,
            ),
            "test",
        )
        assertEquals(0, steps.size)
    }

    /** 没有 pick 段时同样视为未配置。 */
    @Test
    fun keepsEmptyChainWithoutPickSection() {
        assertEquals(0, PickFilterConfig.parse(null, "test").size)
    }

    /** `filter.entity` 必须写成配置段。 */
    @Test
    fun rejectsScalarEntitySection() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PickFilterConfig.parse(
                pickSection(
                    """
                    type: fc.entity
                    max-duration-ticks: 20
                    filter:
                      entity: alive
                    """,
                ),
                "test",
            )
        }
        val message = error.message
        if (message == null) {
            throw AssertionError("expected a message")
        }
        if (!message.contains("filter.entity must be a section")) {
            throw AssertionError("unexpected message: $message")
        }
    }

    /** 不支持的过滤类别直接报错，避免配置写了却被静默忽略。 */
    @Test
    fun rejectsUnknownFilterKind() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PickFilterConfig.parse(
                pickSection(
                    """
                    type: fc.position
                    max-duration-ticks: 20
                    filter:
                      position:
                        targeters:
                          - '@alive'
                    """,
                ),
                "test",
            )
        }
        val message = error.message
        if (message == null) {
            throw AssertionError("expected a message")
        }
        if (!message.contains("only supports 'entity'")) {
            throw AssertionError("unexpected message: $message")
        }
    }

    /** 读取 YAML 里的 `pick` 段。 */
    private fun pickSection(yaml: String): taboolib.library.configuration.ConfigurationSection? {
        val builder = StringBuilder("pick:\n")
        for (line in yaml.trimIndent().lines()) {
            if (line.isBlank()) {
                builder.append('\n')
                continue
            }
            builder.append("  ")
            builder.append(line)
            builder.append('\n')
        }
        val configuration = Configuration.loadFromString(builder.toString())
        return configuration.getConfigurationSection("pick")
    }
}
