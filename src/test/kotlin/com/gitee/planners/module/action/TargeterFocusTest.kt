package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 验证 `@sender` 与 `@sender.loc`。
 *
 * `@sender` 固定指向技能施法者，不随流程目标变化；`self` 是同等语义的别名。
 */
class TargeterFocusTest {

    /**
     * 构造带世界的坐标。
     *
     * @return 世界坐标。
     */
    private fun location(): Location {
        val world = Mockito.mock(World::class.java)
        return Location(world, 1.5, 64.0, 2.5)
    }

    /**
     * 构造查询上下文。
     *
     * @param sender 当前取数主体。
     * @param caster 技能施法者。
     * @param target 绑定的目标。
     * @return 查询上下文。
     */
    private fun scope(sender: Any?, caster: Any? = sender, target: Any? = null): QueryScope {
        return QueryScope(sender = sender, caster = caster, target = target, label = "test")
    }

    /**
     * 解析一个目标器。
     *
     * @param targeter 目标器实现。
     * @param scope 查询上下文。
     * @return 解析结果。
     */
    private fun resolve(targeter: ActionTargeter, scope: QueryScope): List<ActionTarget> {
        return targeter.resolve(scope, ActionArguments(emptyMap(), "test"), emptyList())
    }

    /** @sender 指向施法者，即使流程已经绑定了目标。 */
    @Test
    fun senderPinsCaster() {
        val caster = location()
        val bound = Location(caster.world, 9.0, 70.0, 9.0)
        val resolved = resolve(BuiltinTargeters.Sender, scope(caster, caster, bound))

        assertEquals(1, resolved.size)
        assertEquals(caster, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** 没有施法者时退回当前取数主体。 */
    @Test
    fun senderFallsBackToScopeSender() {
        val sender = location()
        val resolved = resolve(BuiltinTargeters.Sender, scope(sender, null))

        assertEquals(1, resolved.size)
        assertEquals(sender, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** @sender.loc 取施法者坐标。 */
    @Test
    fun senderLocationUsesCaster() {
        val caster = location()
        val resolved = resolve(BuiltinTargeters.SenderLocation, scope(caster))

        assertEquals(1, resolved.size)
        assertEquals(caster, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** 既没有施法者也没有施法主体时返回空容器。 */
    @Test
    fun returnsEmptyWithoutCaster() {
        assertTrue(resolve(BuiltinTargeters.Sender, scope(null, null)).isEmpty())
        assertTrue(resolve(BuiltinTargeters.SenderLocation, scope(null, null)).isEmpty())
    }
}
