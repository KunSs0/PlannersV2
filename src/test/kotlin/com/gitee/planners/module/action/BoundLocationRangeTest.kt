package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

/**
 * 验证几何目标器以"绑定的坐标"为原点。
 *
 * 定时任务把 `@this.loc` 冻结成坐标目标之后，`@range` 应当围绕该坐标选区，
 * 而不是围绕施法者当前位置。
 */
class BoundLocationRangeTest {

    /** 每个用例前恢复内置目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /**
     * 构造坐标。
     *
     * @param world 世界。
     * @param x X 坐标。
     * @param y Y 坐标。
     * @param z Z 坐标。
     * @return 坐标。
     */
    private fun location(world: World, x: Double, y: Double, z: Double): Location {
        return Location(world, x, y, z)
    }

    /**
     * 构造生物目标。
     *
     * @param where 生物所在坐标。
     * @return 生物替身。
     */
    private fun entity(where: Location): LivingEntity {
        val entity = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(entity.location).thenReturn(where)
        Mockito.`when`(entity.uniqueId).thenReturn(UUID.randomUUID())
        return entity
    }

    /** 有绑定坐标时以该坐标为中心，而不是施法者位置。 */
    @Test
    fun rangeUsesBoundLocationAsOrigin() {
        val world = Mockito.mock(World::class.java)
        val origin = location(world, 0.0, 64.0, 0.0)
        val near = entity(location(world, 1.0, 64.0, 0.0))
        val far = entity(location(world, 40.0, 64.0, 0.0))
        Mockito.`when`(world.getNearbyEntities(origin, 3.0, 3.0, 3.0))
            .thenReturn(listOf<org.bukkit.entity.Entity>(near, far))
        val scope = QueryScope(target = origin, label = "bound")

        val resolved = BuiltinTargeters.Range.resolve(
            scope,
            ActionArguments(mapOf("radius" to "3.0"), "test"),
            emptyList(),
        )

        assertEquals(1, resolved.size)
        assertEquals(near, resolved[0].bukkitEntity())
    }

    /** 没有绑定坐标时仍然以施法者为原点。 */
    @Test
    fun rangeFallsBackToSender() {
        val world = Mockito.mock(World::class.java)
        val sender = entity(location(world, 5.0, 64.0, 0.0))
        val near = entity(location(world, 6.0, 64.0, 0.0))
        val senderLocation = sender.location
        Mockito.`when`(world.getNearbyEntities(senderLocation, 3.0, 3.0, 3.0))
            .thenReturn(listOf<org.bukkit.entity.Entity>(near))
        val scope = QueryScope(sender = sender, label = "sender")

        val resolved = BuiltinTargeters.Range.resolve(
            scope,
            ActionArguments(mapOf("radius" to "3.0"), "test"),
            emptyList(),
        )

        assertEquals(1, resolved.size)
        assertEquals(near, resolved[0].bukkitEntity())
    }
}
