package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

/**
 * 验证区域受伤回调的派发。
 *
 * 只派发覆盖受伤成员的实例；作用域里施法者是区域主、目标是受伤成员，
 * 承伤值与攻击者通过 `<skill.var.*>` 读取。
 */
class AreaDamageCallbackTest {

    /** 默认在线判定。 */
    private lateinit var defaultOwnerOnline: (UUID) -> Boolean

    /** 共享世界。 */
    private lateinit var world: World

    /** 每个用例前重置注册表。 */
    @BeforeEach
    fun setUp() {
        defaultOwnerOnline = AreaRegistry.ownerOnline
        AreaRegistry.ownerOnline = { true }
        AreaRegistry.clear()
        world = Mockito.mock(World::class.java)
    }

    /** 用例结束后恢复。 */
    @AfterEach
    fun tearDown() {
        AreaRegistry.clear()
        AreaRegistry.ownerOnline = defaultOwnerOnline
    }

    /**
     * 构造固定圆心的区域实例。
     *
     * @param center 圆心。
     * @param radius 半径。
     * @return 区域实例。
     */
    private fun area(center: Location, radius: Double): AreaInstance {
        return AreaInstance(
            UUID.randomUUID(),
            "bulwark",
            UUID.randomUUID(),
            radius,
            2.0,
            center,
            null,
            System.currentTimeMillis() + 10_000L,
        )
    }

    /**
     * 构造带坐标的实体。
     *
     * @param where 坐标。
     * @return 实体替身。
     */
    private fun entity(where: Location): LivingEntity {
        val entity = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(entity.location).thenReturn(where)
        Mockito.`when`(entity.isValid).thenReturn(true)
        return entity
    }

    /** 覆盖成员的实例会收到回调，作用域绑定正确。 */
    @Test
    fun dispatchesToCoveringArea() {
        val center = Location(world, 0.0, 64.0, 0.0)
        val instance = area(center, 5.0)
        val captured = QueryScope(label = "area", variables = mutableMapOf("threatDamageRatio" to 0.5))
        var invokedFlow: String? = null
        var capturedTarget: Any? = null
        var capturedDamage: Any? = null
        var capturedRatio: Any? = null
        instance.captured = captured
        instance.damagedFlow = "memberDamaged"
        instance.runner = { flow, child ->
            invokedFlow = flow
            capturedTarget = child.target
            capturedDamage = child.variables["damage"]
            capturedRatio = child.variables["threatDamageRatio"]
        }
        AreaRegistry.register(instance)

        val victim = entity(Location(world, 1.0, 64.0, 1.0))
        AreaRegistry.dispatchDamaged(victim, null, 40.0, "skill")

        assertEquals("memberDamaged", invokedFlow)
        assertEquals(victim, capturedTarget)
        assertEquals(40.0, capturedDamage)
        assertEquals(0.5, capturedRatio)
    }

    /** 不在区域内的成员不会触发回调。 */
    @Test
    fun skipsOutsideArea() {
        val center = Location(world, 0.0, 64.0, 0.0)
        val instance = area(center, 3.0)
        var invoked = false
        instance.captured = QueryScope(label = "area")
        instance.damagedFlow = "memberDamaged"
        instance.runner = { _, _ ->
            invoked = true
        }
        AreaRegistry.register(instance)

        AreaRegistry.dispatchDamaged(entity(Location(world, 20.0, 64.0, 20.0)), null, 10.0, null)

        assertTrue(!invoked)
    }
}
