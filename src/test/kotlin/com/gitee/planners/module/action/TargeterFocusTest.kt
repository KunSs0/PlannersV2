package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 验证 `@this` 与坐标目标器。
 *
 * `@this` 默认等同 `@self`；被流程绑定目标后指向该目标，可能是实体也可能是坐标。
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
     * @param sender 施法者。
     * @param target 绑定的目标。
     * @return 查询上下文。
     */
    private fun scope(sender: Any?, target: Any? = null): QueryScope {
        return QueryScope(sender = sender, target = target, label = "test")
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

    /** 没有绑定目标时 @this 等同 @self：坐标施法者得到坐标目标。 */
    @Test
    fun thisFallsBackToSender() {
        val origin = location()
        val resolved = resolve(BuiltinTargeters.This, scope(origin))

        assertEquals(1, resolved.size)
        assertEquals(origin, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** 绑定坐标目标后 @this 指向该坐标。 */
    @Test
    fun thisPrefersBoundTarget() {
        val origin = location()
        val bound = Location(origin.world, 9.0, 70.0, 9.0)
        val resolved = resolve(BuiltinTargeters.This, scope(origin, bound))

        assertEquals(1, resolved.size)
        assertEquals(bound, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** @self.loc 取施法者坐标。 */
    @Test
    fun selfLocationUsesSender() {
        val origin = location()
        val resolved = resolve(BuiltinTargeters.SelfLocation, scope(origin))

        assertEquals(1, resolved.size)
        assertEquals(origin, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** @this.loc 没有绑定目标时取施法者坐标。 */
    @Test
    fun thisLocationFallsBackToSender() {
        val origin = location()
        val resolved = resolve(BuiltinTargeters.ThisLocation, scope(origin))

        assertEquals(1, resolved.size)
        assertEquals(origin, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** @this.loc 绑定坐标后取该坐标。 */
    @Test
    fun thisLocationPrefersBoundTarget() {
        val origin = location()
        val bound = Location(origin.world, 3.0, 65.0, 4.0)
        val resolved = resolve(BuiltinTargeters.ThisLocation, scope(origin, bound))

        assertEquals(1, resolved.size)
        assertEquals(bound, (resolved[0] as ActionTarget.LocationTarget).location)
    }

    /** 没有施法者也没有绑定目标时返回空容器。 */
    @Test
    fun returnsEmptyWithoutFocus() {
        val resolved = resolve(BuiltinTargeters.This, scope(null))
        val located = resolve(BuiltinTargeters.ThisLocation, scope(null))

        assertTrue(resolved.isEmpty())
        assertTrue(located.isEmpty())
    }
}
