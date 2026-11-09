package com.gitee.planners.module.action

import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

/**
 * 验证区域注册表的几何判定、多实例并存与清理。
 *
 * 注册表只回答几何问题，实例之间互不覆盖；清理覆盖到期、跟随实体失效与区域主离线三种情况。
 */
class AreaRegistryTest {

    /** 保存默认的在线判定，测试结束后恢复。 */
    private lateinit var defaultOwnerOnline: (UUID) -> Boolean

    /** 共享的假世界。 */
    private lateinit var world: World

    /** 每个用例前重置注册表并替换在线判定。 */
    @BeforeEach
    fun setUp() {
        defaultOwnerOnline = AreaRegistry.ownerOnline
        AreaRegistry.ownerOnline = { true }
        AreaRegistry.clear()
        world = Mockito.mock(World::class.java)
    }

    /** 用例结束后恢复注册表状态。 */
    @AfterEach
    fun tearDown() {
        AreaRegistry.clear()
        AreaRegistry.ownerOnline = defaultOwnerOnline
    }

    /**
     * 构造固定圆心的实例。
     *
     * @param id 分类名。
     * @param owner 区域主。
     * @param center 圆心。
     * @param radius 半径。
     * @param height 上下容差。
     * @param expiresAt 过期时刻。
     * @return 区域实例。
     */
    private fun area(
        id: String,
        owner: UUID,
        center: Location,
        radius: Double = 3.0,
        height: Double = 2.0,
        expiresAt: Long = System.currentTimeMillis() + 10_000L,
    ): AreaInstance {
        return AreaInstance(UUID.randomUUID(), id, owner, radius, height, center, null, expiresAt)
    }

    /** 半径与高度共同决定命中。 */
    @Test
    fun matchesRadiusAndHeight() {
        val center = Location(world, 0.0, 64.0, 0.0)
        AreaRegistry.register(area("guard", UUID.randomUUID(), center))

        assertTrue(AreaRegistry.contains(Location(world, 2.0, 64.0, 0.0), "guard"))
        assertTrue(AreaRegistry.contains(Location(world, 0.0, 66.0, 0.0), "guard"))
        assertFalse(AreaRegistry.contains(Location(world, 0.0, 67.0, 0.0), "guard"))
        assertFalse(AreaRegistry.contains(Location(world, 3.5, 64.0, 0.0), "guard"))
        assertFalse(AreaRegistry.contains(Location(world, 0.0, 64.0, 0.0), "bulwark"))
    }

    /** 不同世界不命中。 */
    @Test
    fun ignoresOtherWorlds() {
        val center = Location(world, 0.0, 64.0, 0.0)
        AreaRegistry.register(area("guard", UUID.randomUUID(), center))
        val otherWorld = Mockito.mock(World::class.java)

        assertFalse(AreaRegistry.contains(Location(otherWorld, 0.0, 64.0, 0.0), "guard"))
    }

    /** 同一分类允许多份实例并存，计数如实反映。 */
    @Test
    fun countsParallelInstances() {
        val center = Location(world, 0.0, 64.0, 0.0)
        AreaRegistry.register(area("guard", UUID.randomUUID(), center))
        AreaRegistry.register(area("guard", UUID.randomUUID(), center))
        AreaRegistry.register(area("bulwark", UUID.randomUUID(), center))

        assertEquals(2, AreaRegistry.count(center, "guard"))
        assertEquals(1, AreaRegistry.count(center, "bulwark"))
        assertEquals(0, AreaRegistry.count(center, "unknown"))
    }

    /** 停止只影响自己的实例。 */
    @Test
    fun stopOwnedOnlyRemovesOwn() {
        val owner = UUID.randomUUID()
        val other = UUID.randomUUID()
        val center = Location(world, 0.0, 64.0, 0.0)
        AreaRegistry.register(area("guard", owner, center))
        AreaRegistry.register(area("guard", owner, center))
        AreaRegistry.register(area("guard", other, center))

        assertEquals(2, AreaRegistry.stopOwned(owner, "guard"))
        assertEquals(1, AreaRegistry.count(center, "guard"))
    }

    /** 到期实例在访问时被清理。 */
    @Test
    fun purgesExpiredInstances() {
        val center = Location(world, 0.0, 64.0, 0.0)
        AreaRegistry.register(area("guard", UUID.randomUUID(), center, expiresAt = System.currentTimeMillis() - 1L))

        assertEquals(0, AreaRegistry.count(center, "guard"))
        assertEquals(0, AreaRegistry.size())
    }

    /** 区域主离线后实例被清理。 */
    @Test
    fun purgesOfflineOwner() {
        val center = Location(world, 0.0, 64.0, 0.0)
        AreaRegistry.register(area("guard", UUID.randomUUID(), center))
        AreaRegistry.ownerOnline = { false }

        assertEquals(0, AreaRegistry.count(center, "guard"))
        assertEquals(0, AreaRegistry.size())
    }
}
