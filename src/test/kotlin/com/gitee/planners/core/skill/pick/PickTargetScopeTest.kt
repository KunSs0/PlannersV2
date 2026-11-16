package com.gitee.planners.core.skill.pick

import com.gitee.planners.api.pick.PickEntityResult
import com.gitee.planners.api.pick.PickLocResult
import com.gitee.planners.api.pick.PickResult
import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionRegistry
import com.gitee.planners.module.action.ActionScopeFactory
import com.gitee.planners.module.action.ActionTargeterStep
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

/**
 * 验证指向确认结果到动作目标的绑定。
 *
 * 断言 `@target` 目标器与 `<target.*>` 查询根读到同一个服务端确认实体；不启动服务端。
 */
class PickTargetScopeTest {

    /** 每个用例前恢复内置目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 实体结果被绑定成当前目标。 */
    @Test
    fun bindsEntityResult() {
        val player = player()
        val result = PickEntityResult(player)
        val bound = ActionScopeFactory.resolvePickTarget(result)
        assertSame(player, bound)
    }

    /** 非实体结果不参与目标绑定。 */
    @Test
    fun ignoresOtherResult() {
        val result = object : PickResult {
            override val type = "unknown"
        }
        assertNull(ActionScopeFactory.resolvePickTarget(result))
    }

    /** 位置结果被绑定成坐标目标。 */
    @Test
    fun bindsLocResult() {
        val location = Location(null, 12.0, 64.0, -3.0)
        val result = PickLocResult(location)
        assertSame(location, ActionScopeFactory.resolvePickTarget(result))
    }

    /** 坐标结果经 `@target` 变成坐标目标，供 area 锚点固定使用。 */
    @Test
    fun currentTargeterReturnsBoundLocation() {
        val player = player()
        val location = Location(Mockito.mock(World::class.java), 12.0, 64.0, -3.0)
        val scope = QueryScope(sender = player, target = location, label = "test")
        val targeter = ActionRegistry.targeter("target")
        if (targeter == null) {
            throw AssertionError("builtin targeter 'target' must be registered")
        }
        val step = ActionTargeterStep(targeter, ActionArguments(emptyMap(), "test"), "test")
        val targets = step.apply(scope, emptyList())
        assertEquals(1, targets.size)
        val bound = targets[0] as ActionTarget.LocationTarget
        assertEquals(location, bound.location)
    }

    /** 未确认指向时保持无目标。 */
    @Test
    fun ignoresMissingResult() {
        assertNull(ActionScopeFactory.resolvePickTarget(null))
    }

    /** 查询根从当前目标读取字段。 */
    @Test
    fun readsTargetFieldsFromScope() {
        val player = player()
        val scope = QueryScope(sender = player, target = player, label = "test")
        assertEquals(player.uniqueId.toString(), QueryRef.parse("<target.uuid>", "test").resolveText(scope))
    }

    /** `@target` 目标器返回绑定的目标。 */
    @Test
    fun currentTargeterReturnsBoundTarget() {
        val player = player()
        val scope = QueryScope(sender = player, target = player, label = "test")
        val targeter = ActionRegistry.targeter("target")
        if (targeter == null) {
            throw AssertionError("builtin targeter 'target' must be registered")
        }
        val step = ActionTargeterStep(targeter, ActionArguments(emptyMap(), "test"), "test")
        val targets = step.apply(scope, emptyList())
        assertEquals(1, targets.size)
        assertEquals(player.uniqueId, targets[0].bukkitEntity()?.uniqueId)
    }

    /** 构造一个在线玩家替身。 */
    private fun player(): Player {
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.uniqueId).thenReturn(UUID.randomUUID())
        Mockito.`when`(player.isOnline).thenReturn(true)
        Mockito.`when`(player.name).thenReturn("target")
        return player
    }
}
