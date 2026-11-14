package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

/**
 * 验证光束选区用的 `@rect` 几何。
 *
 * 光束按「施法者身体朝向 + 前移起点」的等宽长方体判定：
 * 只有落在长度区间、左右半宽与上下半高内的实体被选中。
 */
class RectBeamTest {

    /** 每个用例前恢复内置目标器。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 前方区间内的实体被选中，越过长度、偏出宽度或位于身后的实体被排除。 */
    @Test
    fun selectsOnlyTargetsInsideBeamBox() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 0.0, 64.0, 0.0)
        val caster = caster(origin)
        val inside = entity(world, 0.0, 64.0, 7.5)
        val beyondLength = entity(world, 0.0, 64.0, 20.0)
        val beside = entity(world, 2.0, 64.0, 7.5)
        val behind = entity(world, 0.0, 64.0, 0.0)
        Mockito.`when`(
            world.getNearbyEntities(
                Mockito.any(Location::class.java),
                Mockito.anyDouble(),
                Mockito.anyDouble(),
                Mockito.anyDouble(),
            ),
        ).thenReturn(listOf(inside, beyondLength, beside, behind))

        val resolved = BuiltinTargeters.Rect.resolve(
            QueryScope(sender = caster, caster = caster, label = "beam"),
            ActionArguments(
                mapOf(
                    "width" to "1.5",
                    "height" to "2.0",
                    "length" to "11.0",
                    "offsetY" to "0.0",
                    "offsetZ" to "7.5",
                ),
                "test.rect",
            ),
            emptyList(),
        )


        assertEquals(1, resolved.size)
        assertEquals(inside, resolved[0].bukkitEntity())
    }

    /** 构造朝向 +Z 的施法者替身。 */
    private fun caster(where: Location): Player {
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.location).thenReturn(where)
        Mockito.`when`(player.width).thenReturn(0.6)
        Mockito.`when`(player.height).thenReturn(1.8)
        Mockito.`when`(player.uniqueId).thenReturn(UUID.randomUUID())
        return player
    }

    /** 构造带固定尺寸的生物替身。 */
    private fun entity(world: World, x: Double, y: Double, z: Double): LivingEntity {
        val entity = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(entity.location).thenReturn(Location(world, x, y, z))
        Mockito.`when`(entity.width).thenReturn(0.6)
        Mockito.`when`(entity.height).thenReturn(1.8)
        Mockito.`when`(entity.uniqueId).thenReturn(UUID.randomUUID())
        return entity
    }
}
