package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock

/**
 * 验证 `distance` 通用过滤规则。
 *
 * 规则只负责「已经选出来的目标离施法者多远」，因此可以叠在 `@online-players`、
 * `@target.finder` 这类非选区容器上；距离取施法者与目标的三维距离，跨世界直接淘汰。
 */
class DistanceRuleTest {

    private lateinit var server: ServerMock

    /** 每个用例前启动 MockBukkit 并恢复内置规则。 */
    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        FilterRules.reset()
    }

    /** 每个用例后关闭 MockBukkit。 */
    @AfterEach
    fun reset() {
        MockBukkit.unmock()
    }

    /** 规则登记在内置注册表里。 */
    @Test
    fun registersBuiltinRule() {
        assertTrue(FilterRules.ids.contains("distance"))
    }

    /** 上限是闭区间，超出则淘汰；下限同样按闭区间。 */
    @Test
    fun keepsTargetsInsideBounds() {
        val caster = player("Caster", 0.0, 64.0, 0.0)
        val near = player("Near", 3.0, 64.0, 0.0)
        val far = player("Far", 9.0, 64.0, 0.0)

        assertTrue(test(caster, near, mapOf("max" to "3")))
        assertFalse(test(caster, far, mapOf("max" to "3")))
        assertTrue(test(caster, far, mapOf("max" to "9")))
        assertFalse(test(caster, near, mapOf("min" to "4")))
        assertTrue(test(caster, near, mapOf("min" to "2", "max" to "4")))
    }

    /** 距离按三维算，垂直方向同样计入。 */
    @Test
    fun countsVerticalDistance() {
        val caster = player("Caster", 0.0, 64.0, 0.0)
        val above = player("Above", 0.0, 70.0, 0.0)

        assertFalse(test(caster, above, mapOf("max" to "5")))
        assertTrue(test(caster, above, mapOf("max" to "6")))
    }

    /** 跨世界没有可比距离，直接淘汰而不是抛异常。 */
    @Test
    fun rejectsOtherWorld() {
        val caster = player("Caster", 0.0, 64.0, 0.0)
        val other = server.addSimpleWorld("other")
        val target = server.addPlayer("Other")
        target.teleport(Location(other, 0.0, 64.0, 0.0))

        assertFalse(test(caster, target, mapOf("max" to "100")))
    }

    /** 缺少区间、区间反转、负端点与未知参数都在加载期拒绝。 */
    @Test
    fun rejectsInvalidBounds() {
        assertThrows<IllegalArgumentException> {
            DistanceRule.validate(ActionArguments(emptyMap(), "test.distance"))
        }
        assertThrows<IllegalArgumentException> {
            DistanceRule.validate(ActionArguments(mapOf("min" to "6", "max" to "2"), "test.distance"))
        }
        assertThrows<IllegalArgumentException> {
            DistanceRule.validate(ActionArguments(mapOf("max" to "-1"), "test.distance"))
        }
        assertThrows<IllegalArgumentException> {
            DistanceRule.validate(ActionArguments(mapOf("max" to "1", "extra" to "1"), "test.distance"))
        }
    }

    /** 用规则判定单个目标。 */
    private fun test(caster: PlayerMock, target: PlayerMock, values: Map<String, String>): Boolean {
        val arguments = ActionArguments(values, "test.distance")
        DistanceRule.validate(arguments)
        return DistanceRule.test(scope(caster), ActionTarget.PlayerTarget(target), arguments, null)
    }

    /** 构造只绑定施法者的上下文。 */
    private fun scope(caster: PlayerMock): QueryScope {
        return QueryScope(sender = caster, caster = caster, label = "distance")
    }

    /** 构造处于指定坐标的玩家。 */
    private fun player(name: String, x: Double, y: Double, z: Double): PlayerMock {
        val player = server.addPlayer(name)
        val world: World = server.getWorlds().first()
        player.teleport(Location(world, x, y, z))
        return player
    }
}
