package com.gitee.planners.core.skill.pick

import com.gitee.planners.api.pick.DirectionPickDefinition
import com.gitee.planners.api.pick.EntityPickDefinition
import com.gitee.planners.api.pick.PickCloseReason
import com.gitee.planners.api.pick.PickContext
import com.gitee.planners.api.pick.PickDirectionInput
import com.gitee.planners.api.pick.PickDirectionResult
import com.gitee.planners.api.pick.PickEntityInput
import com.gitee.planners.api.pick.PickEntityResult
import com.gitee.planners.api.pick.PickLocResult
import com.gitee.planners.api.pick.PickDefinition
import com.gitee.planners.api.pick.PickPositionInput
import com.gitee.planners.api.pick.PickPresentation
import com.gitee.planners.api.pick.PickResult
import com.gitee.planners.api.pick.PositionPickDefinition
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockito.Mockito

/**
 * Planners 自建的指向会话必须重新校验客户端上报，而不是直接信任候选。
 *
 * 覆盖位置（距离、地面）、实体（夹角、存活）与方向（俯仰角）三条路径，
 * 以及表现后端的 open/close 调用。
 */
class PlannersPickSessionTest {

    private lateinit var server: ServerMock
    private lateinit var presentation: RecordingPresentation

    /**
     * 准备 MockBukkit 与记录型表现后端。
     */
    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        presentation = RecordingPresentation()
    }

    /** 每个用例后关闭 MockBukkit。 */
    @AfterEach
    fun reset() {
        MockBukkit.unmock()
    }

    /** 合法落点应通过，并在开始与结束时通知表现后端。 */
    @Test
    fun confirmPositionAcceptsStandingPoint() {
        val definition = PositionPickDefinition(200, 16.0, "block-surface", true)
        val session = session(definition, playerWithFloor())
        session.begin()
        val result = session.confirm(PickPositionInput(1L, 0.5, 64.0, 0.5))
        assertNotNull(result)
        assertTrue(result is PickLocResult)
        session.close(PickCloseReason.CONFIRMED)
        assertEquals(1, presentation.opened)
        assertEquals(1, presentation.closed)
    }

    /** 超过距离上限的落点必须拒绝。 */
    @Test
    fun confirmPositionRejectsTooFar() {
        val definition = PositionPickDefinition(200, 4.0, "block-surface", false)
        val session = session(definition, playerWithFloor())
        val result = session.confirm(PickPositionInput(1L, 12.5, 64.0, 0.5))
        assertNull(result)
    }

    /** 落点下方不是实心方块时必须拒绝。 */
    @Test
    fun confirmPositionRejectsWithoutFloor() {
        val definition = PositionPickDefinition(200, 16.0, "block-surface", false)
        val world = Mockito.mock(World::class.java)
        mockBlocks(world, 0)
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.world).thenReturn(world)
        Mockito.`when`(player.eyeLocation).thenReturn(Location(world, 0.5, 64.0, 0.5))
        val session = session(definition, player)
        val result = session.confirm(PickPositionInput(1L, 0.5, 64.0, 0.5))
        assertNull(result)
    }

    /** 夹角在上限内且上下文的实体可用时通过。 */
    @Test
    fun confirmEntityAcceptsWithinAngle() {
        val world = server.addSimpleWorld("world")
        val zombie = world.spawnEntity(Location(world, 0.5, 64.0, 3.0), EntityType.ZOMBIE)
        val definition = EntityPickDefinition(40, 16.0, 30.0, false)
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.world).thenReturn(world)
        Mockito.`when`(player.eyeLocation).thenReturn(Location(world, 0.5, 65.0, 0.5))
        val session = session(definition, player)
        val aim = targetDirection(player.eyeLocation, zombie)
        val result = session.confirm(PickEntityInput(1L, zombie.uniqueId, aim[0], aim[1], aim[2]))
        assertNotNull(result)
        assertTrue(result is PickEntityResult)
    }

    /** 相机朝向与目标方向夹角超限时必须拒绝。 */
    @Test
    fun confirmEntityRejectsBeyondAngle() {
        val world = server.addSimpleWorld("world")
        val zombie = world.spawnEntity(Location(world, 0.5, 64.0, 3.0), EntityType.ZOMBIE)
        val definition = EntityPickDefinition(40, 16.0, 30.0, false)
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.world).thenReturn(world)
        Mockito.`when`(player.eyeLocation).thenReturn(Location(world, 0.5, 65.0, 0.5))
        val session = session(definition, player)
        val result = session.confirm(PickEntityInput(1L, zombie.uniqueId, 0.0, 0.0, -1.0))
        assertNull(result)
    }

    /** 俯仰角超过上限的方向必须拒绝，水平方向必须通过。 */
    @Test
    fun confirmDirectionChecksPitchLimit() {
        val definition = DirectionPickDefinition(30, 30.0)
        val session = session(definition, playerWithFloor())
        assertNull(session.confirm(PickDirectionInput(1L, 0.0, 1.0, 0.0)))
        val result: PickResult? = session.confirm(PickDirectionInput(1L, 1.0, 0.0, 0.0))
        assertTrue(result is PickDirectionResult)
    }

    /**
     * 组装会话。
     *
     * @param definition 本次定义。
     * @param player 施法者替身。
     * @return 会话实例。
     */
    private fun session(definition: PickDefinition, player: Player): PlannersPickSession {
        val skill = Mockito.mock(PlayerSkill::class.java)
        val context = PickContext(1L, player, skill, definition, "keyboard.f", null)
        return PlannersPickSession(context, presentation)
    }

    /**
     * 构造一个站得住的地面世界：y=63 实心，其余可通行。
     *
     * @return 施法者替身。
     */
    private fun playerWithFloor(): Player {
        val world = Mockito.mock(World::class.java)
        mockBlocks(world, 63)
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.world).thenReturn(world)
        Mockito.`when`(player.eyeLocation).thenReturn(Location(world, 0.5, 64.0, 0.5))
        Mockito.`when`(player.hasLineOfSight(Mockito.any(Entity::class.java))).thenReturn(true)
        return player
    }

    /**
     * 让方块查询在指定高度以下返回实心方块。
     *
     * @param world 世界替身。
     * @param solidTop 实心方块的最大 Y。
     */
    private fun mockBlocks(world: World, solidTop: Int) {
        Mockito.`when`(world.getBlockAt(Mockito.any(Location::class.java))).thenAnswer { invocation ->
            val location = invocation.arguments[0] as Location
            val solid = location.blockY <= solidTop
            val block = Mockito.mock(Block::class.java)
            Mockito.`when`(block.type).thenReturn(if (solid) Material.STONE else Material.AIR)
            Mockito.`when`(block.isPassable).thenReturn(!solid)
            block
        }
    }

    /**
     * 计算施法者眼睛指向实体中心的方向。
     *
     * @param eye 施法者眼睛位置。
     * @param entity 目标实体。
     * @return 归一化方向分量数组。
     */
    private fun targetDirection(eye: Location, entity: Entity): DoubleArray {
        val dx = entity.location.x - eye.x
        val dy = entity.location.y + entity.height / 2.0 - eye.y
        val dz = entity.location.z - eye.z
        val length = Math.sqrt(dx * dx + dy * dy + dz * dz)
        return doubleArrayOf(dx / length, dy / length, dz / length)
    }

    /** 记录 open/close 次数的表现后端替身。 */
    private class RecordingPresentation : PickPresentation {

        /** open 调用次数。 */
        var opened: Int = 0

        /** close 调用次数。 */
        var closed: Int = 0

        override fun open(context: PickContext) {
            opened += 1
        }

        override fun close(context: PickContext, reason: PickCloseReason) {
            closed += 1
        }
    }
}
