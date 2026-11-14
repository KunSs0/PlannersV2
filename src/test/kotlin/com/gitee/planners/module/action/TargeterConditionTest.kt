package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import taboolib.module.configuration.Configuration

/**
 * 验证 `targeter` 条件：目标器链命中即成立，可用于「有目标走 A、否则走 B」的流程分支。
 */
class TargeterConditionTest {

    /** 每个用例前恢复内置条目并注册命中/未命中的测试目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(HitTargeter)
        ActionRegistry.register(MissTargeter)
    }

    /** 链命中时条件成立。 */
    @Test
    fun matchesWhenChainProducesTarget() {
        val condition = PlannersConditions.Targeter
        val arguments = ActionArguments(mapOf("e" to "@test.hit"), "test.condition")
        val compiled = condition.compile(arguments)
        assertTrue(condition.test(scope(), arguments, compiled))
    }

    /** 链中任意一步清空容器即不成立。 */
    @Test
    fun rejectsWhenChainDropsTarget() {
        val condition = PlannersConditions.Targeter
        val arguments = ActionArguments(mapOf("e" to "@test.hit @test.miss"), "test.condition")
        val compiled = condition.compile(arguments)
        assertFalse(condition.test(scope(), arguments, compiled))
    }

    /** 缺少参数时加载期报错。 */
    @Test
    fun requiresChainArgument() {
        val condition = PlannersConditions.Targeter
        val error = runCatching { condition.compile(ActionArguments(emptyMap(), "test.condition")) }.exceptionOrNull()
        assertNotNull(error)
    }

    /** 条件条目支持末尾 `false` 取反，供「没有目标」的流程使用。 */
    @Test
    fun flowConditionSupportsNegation() {
        val configuration = Configuration.loadFromString(
            """
            action:
              noTarget:
                conditions: ['targeter{e="@test.miss"} false']
                actions:
                  - 'message{m="ok"}'
            """.trimIndent(),
        )
        val spec = ActionParser.parse("test", configuration.getConfigurationSection("action"), "action")
        if (spec == null) {
            throw AssertionError("flow must be parsed")
        }
        val flow = spec.flow("noTarget")
        if (flow == null) {
            throw AssertionError("flow must exist")
        }
        assertEquals(1, flow.conditions.size)
        assertTrue(flow.conditions[0].matches(scope()))
    }

    /** 构造只带标签的上下文。 */
    private fun scope(): QueryScope {
        return QueryScope(label = "test")
    }

    /** 忽略输入直接产出一个实体的目标器。 */
    object HitTargeter : ActionTargeter {

        override val id = "test.hit"

        override val aliases = listOf("test.hit")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val entity = Mockito.mock(LivingEntity::class.java)
            Mockito.`when`(entity.isValid).thenReturn(true)
            Mockito.`when`(entity.isDead).thenReturn(false)
            return listOf(ActionTarget.EntityTarget(entity))
        }
    }

    /** 清空容器的目标器。 */
    object MissTargeter : ActionTargeter {

        override val id = "test.miss"

        override val aliases = listOf("test.miss")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            return emptyList()
        }
    }
}
