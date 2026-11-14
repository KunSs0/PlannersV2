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
 * 验证 `@range` 的球形与圆柱两种判定。
 *
 * 不给 `height` 时维持球形半径；给了 `height` 时按「水平半径 + 垂直半高」的圆柱判定，
 * 与 `area.start` 的场地语义一致。
 */
class RangeCylinderTest {

    /** 每个用例前恢复内置目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 圆柱判定按垂直半高过滤，球形会误判的高处目标被排除。 */
    @Test
    fun cylinderExcludesTargetsAboveHeight() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 0.0, 64.0, 0.0)
        val inside = entity(Location(world, 2.0, 65.5, 0.0))
        val tooHigh = entity(Location(world, 0.0, 67.0, 0.0))
        Mockito.`when`(world.getNearbyEntities(origin, 4.0, 2.0, 4.0))
            .thenReturn(listOf<org.bukkit.entity.Entity>(inside, tooHigh))
        val scope = QueryScope(target = origin, label = "area")

        val resolved = BuiltinTargeters.Range.resolve(
            scope,
            ActionArguments(mapOf("radius" to "4.0", "height" to "2.0"), "test"),
            emptyList(),
        )

        assertEquals(1, resolved.size)
        assertEquals(inside, resolved[0].bukkitEntity())
    }

    /** 圆柱判定按水平半径过滤，远处目标被排除。 */
    @Test
    fun cylinderExcludesTargetsBeyondRadius() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 0.0, 64.0, 0.0)
        val inside = entity(Location(world, 3.9, 64.0, 0.0))
        val outside = entity(Location(world, 4.5, 64.0, 0.0))
        Mockito.`when`(world.getNearbyEntities(origin, 4.0, 2.0, 4.0))
            .thenReturn(listOf<org.bukkit.entity.Entity>(inside, outside))
        val scope = QueryScope(target = origin, label = "area")

        val resolved = BuiltinTargeters.Range.resolve(
            scope,
            ActionArguments(mapOf("radius" to "4.0", "height" to "2.0"), "test"),
            emptyList(),
        )

        assertEquals(1, resolved.size)
        assertEquals(inside, resolved[0].bukkitEntity())
    }

    /** 不给 height 时仍是球形：正上方且距离在半径内的目标保留。 */
    @Test
    fun keepsSphereWithoutHeight() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 0.0, 64.0, 0.0)
        val above = entity(Location(world, 0.0, 66.5, 0.0))
        Mockito.`when`(world.getNearbyEntities(origin, 4.0, 4.0, 4.0))
            .thenReturn(listOf<org.bukkit.entity.Entity>(above))
        val scope = QueryScope(target = origin, label = "area")

        val resolved = BuiltinTargeters.Range.resolve(
            scope,
            ActionArguments(mapOf("radius" to "4.0"), "test"),
            emptyList(),
        )

        assertEquals(1, resolved.size)
    }

    /** 非正的 height 直接返回空集，不做静默降级。 */
    @Test
    fun rejectsNonPositiveHeight() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 0.0, 64.0, 0.0)
        val scope = QueryScope(target = origin, label = "area")

        val resolved = BuiltinTargeters.Range.resolve(
            scope,
            ActionArguments(mapOf("radius" to "4.0", "height" to "0"), "test"),
            emptyList(),
        )

        assertEquals(0, resolved.size)
    }

    /** 构造生物替身。 */
    private fun entity(where: Location): LivingEntity {
        val entity = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(entity.location).thenReturn(where)
        Mockito.`when`(entity.uniqueId).thenReturn(UUID.randomUUID())
        return entity
    }
}
