package com.gitee.planners.core.skill.directing

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionRegistry
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.ActionTargeter
import com.gitee.planners.module.action.ActionTargeterChainParser
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 验证过滤链对单个候选的执行结果。
 *
 * 使用 Mockito 构造实体，不启动服务端；新增的测试目标器在每个用例后撤销。
 */
class DirectingTargeterFilterTest {

    /** 每个用例前恢复内置目标器并注册一个固定拒绝的目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(RejectTargeter)
    }

    /** 用例结束后撤销测试目标器，避免影响其他用例。 */
    @AfterEach
    fun cleanup() {
        ActionRegistry.resetToBuiltins()
    }

    /** `@alive` 保留存活候选。 */
    @Test
    fun keepsAliveCandidate() {
        val entity = livingEntity(true, false)
        val filter = DirectingTargeterFilter(QueryScope(label = "test"), steps("@alive"))
        assertTrue(filter.accept(entity))
    }

    /** `@alive` 拒绝死亡候选。 */
    @Test
    fun rejectsDeadCandidate() {
        val entity = livingEntity(true, true)
        val filter = DirectingTargeterFilter(QueryScope(label = "test"), steps("@alive"))
        assertFalse(filter.accept(entity))
    }

    /** `@alive` 拒绝非法候选。 */
    @Test
    fun rejectsInvalidCandidate() {
        val entity = livingEntity(false, false)
        val filter = DirectingTargeterFilter(QueryScope(label = "test"), steps("@alive"))
        assertFalse(filter.accept(entity))
    }

    /** 链上任意一步清空容器即拒绝；顺序按声明执行。 */
    @Test
    fun rejectsWhenAnyStepDropsCandidate() {
        val entity = livingEntity(true, false)
        val filter = DirectingTargeterFilter(QueryScope(label = "test"), steps("@alive", "@filter.reject"))
        assertFalse(filter.accept(entity))
    }

    /** 构造一个存活状态可控的实体替身。 */
    private fun livingEntity(valid: Boolean, dead: Boolean): LivingEntity {
        val entity = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(entity.isValid).thenReturn(valid)
        Mockito.`when`(entity.isDead).thenReturn(dead)
        return entity
    }

    /** 解析一段过滤链文本。 */
    private fun steps(vararg text: String): List<com.gitee.planners.module.action.ActionTargeterStep> {
        return ActionTargeterChainParser.parseEntries(text.toList(), "test.filter")
    }

    /** 固定拒绝任何候选的目标器，用于验证链式短路。 */
    object RejectTargeter : ActionTargeter {

        override val id = "filter.reject"

        override val aliases = listOf("filter.reject")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            return emptyList()
        }
    }
}
