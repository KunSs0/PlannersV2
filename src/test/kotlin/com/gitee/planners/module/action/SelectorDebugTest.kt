package com.gitee.planners.module.action

import com.gitee.planners.api.common.util.RangeNearestEntityFinder
import com.gitee.planners.api.common.util.RectNearestEntityFinder
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 选区调试绘制必须与选区判定共用几何。
 *
 * 开关关闭时不发送任何粒子；开启后只发给施法玩家本人，并且各形状的采样点必须落在
 * 对应查找器的实际判定范围内，不能凭配置参数另算一套形状。
 */
class SelectorDebugTest {

    /** 每个用例结束后关闭调试，避免静态开关与采样参数串场。 */
    @AfterEach
    fun reset() {
        SelectorDebug.apply(false, Particle.END_ROD, 0.5, 0.15)
    }

    /** 开关关闭时即使施法者是玩家也不发送粒子。 */
    @Test
    fun shouldNotDrawWhenDisabled() {
        val player = player()
        val finder = rectFinder(Mockito.mock(World::class.java))

        SelectorDebug.apply(false, Particle.END_ROD, 0.5, 0.15)
        SelectorDebug.drawRect(player, finder)

        assertEquals(0, particleCount(player), "关闭开关不发送粒子")
    }

    /** 开启后按配置粒子绘制，且仅发送给施法玩家。 */
    @Test
    fun shouldDrawForSenderWithConfiguredParticle() {
        val player = player()
        val finder = rectFinder(Mockito.mock(World::class.java))

        SelectorDebug.apply(true, Particle.END_ROD, 0.5, 0.15)
        SelectorDebug.drawRect(player, finder)

        assertTrue(particleCount(player) > 100, "11 格光束框按 0.5 格采样应产生上百个采样点")
        assertEquals(Particle.END_ROD, firstParticle(player), "使用配置的粒子类型")
    }

    /** 重复引导每次都要重新绘制，不能只在首次执行时生效。 */
    @Test
    fun shouldDrawOnEveryCall() {
        val player = player()
        val finder = rectFinder(Mockito.mock(World::class.java))

        SelectorDebug.apply(true, Particle.END_ROD, 0.5, 0.15)
        SelectorDebug.drawRect(player, finder)
        val first = particleCount(player)
        SelectorDebug.drawRect(player, finder)

        assertEquals(first * 2, particleCount(player), "每次执行都完整绘制一遍")
        assertTrue(first > 0, "首次执行必须真的发出粒子")
    }

    /** 非玩家施法者没有可见对象，三个绘制入口都必须安全跳过。 */
    @Test
    fun shouldSkipNonPlayerSender() {
        val world = Mockito.mock(World::class.java)
        SelectorDebug.apply(true, Particle.END_ROD, 0.5, 0.15)

        // 施法者是坐标而不是玩家时没有任何可见对象，入口不得抛错。
        SelectorDebug.drawRect(Location(null, 0.0, 0.0, 0.0), rectFinder(world))
        SelectorDebug.drawSector(Location(null, 0.0, 0.0, 0.0), sectorFinder(world))
        SelectorDebug.drawRange(
            Location(null, 0.0, 0.0, 0.0),
            RangeNearestEntityFinder(Location(world, 0.0, 64.0, 0.0), 3.0, null, emptyList()),
        )
    }

    /** 矩形调试框在任意朝向下都要覆盖 11 × 1.5 × 2.4 的实际判定范围。 */
    @Test
    fun shouldCoverBeamBoxAtEveryFacing() {
        val world = Mockito.mock(World::class.java)
        for (yaw in listOf(0.0f, 90.0f, 180.0f, -90.0f, 37.0f)) {
            val origin = Location(world, 10.0, 20.0, 30.0, yaw, 0.0f)
            val finder = RectNearestEntityFinder(origin, 1.5, 2.4, 11.0, yaw, 0.0, 1.0, 7.5, emptyList())
            val radians = Math.toRadians(yaw.toDouble())
            val forwardX = -sin(radians)
            val forwardZ = cos(radians)
            var minAhead = 100.0
            var maxAhead = -100.0
            var minY = 100.0
            var maxY = -100.0
            var minSide = 100.0
            var maxSide = -100.0
            for (point in SelectorDebug.rectPoints(world, finder, 0.5)) {
                val deltaX = point.x - origin.x
                val deltaZ = point.z - origin.z
                val ahead = deltaX * forwardX + deltaZ * forwardZ
                val side = deltaX * forwardZ - deltaZ * forwardX
                assertTrue(abs(side) <= 0.750001, "框宽与 PL 判定的 1.5 格一致")
                assertTrue(ahead >= 1.999999 && ahead <= 13.000001, "框前端 2 格、后端 13 格")
                if (ahead < minAhead) {
                    minAhead = ahead
                }
                if (ahead > maxAhead) {
                    maxAhead = ahead
                }
                if (point.y < minY) {
                    minY = point.y
                }
                if (point.y > maxY) {
                    maxY = point.y
                }
                if (side < minSide) {
                    minSide = side
                }
                if (side > maxSide) {
                    maxSide = side
                }
            }
            assertEquals(2.0, minAhead, 1.0e-6, "朝向 $yaw 有完整前边界")
            assertEquals(13.0, maxAhead, 1.0e-6, "朝向 $yaw 有完整后边界")
            assertEquals(19.8, minY, 1.0e-6, "框底按中心上移 1 格、高 2.4 格计算")
            assertEquals(22.2, maxY, 1.0e-6, "框顶按中心上移 1 格、高 2.4 格计算")
            assertEquals(1.5, maxSide - minSide, 1.0e-6, "左右完整宽度为 1.5 格")
        }
    }

    /** 扇形调试点必须落在半径与开角内，并抬到配置的绘制平面。 */
    @Test
    fun shouldKeepSectorPointsInsideWedge() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 5.0, 64.0, -3.0, 30.0f, 0.0f)
        val finder = sectorFinder(world)
        val radians = Math.toRadians(30.0)
        val forwardX = -sin(radians)
        val forwardZ = cos(radians)
        var maxDistance = 0.0
        for (point in SelectorDebug.sectorPoints(world, finder, 0.5, 0.15)) {
            assertEquals(64.15, point.y, 1.0e-9, "扇形绘制平面按 y-offset 抬高")
            val deltaX = point.x - origin.x
            val deltaZ = point.z - origin.z
            val distance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
            assertTrue(distance <= 10.000001, "扇形调试点不能超出半径")
            val dot = deltaX * forwardX + deltaZ * forwardZ
            val cross = forwardX * deltaZ - forwardZ * deltaX
            val difference = Math.toDegrees(atan2(cross, dot))
            assertTrue(abs(difference) <= 45.000001, "扇形调试点必须落在开角内")
            if (distance > maxDistance) {
                maxDistance = distance
            }
        }
        assertTrue(abs(maxDistance - 10.0) < 1.0e-6, "圆弧必须画到完整半径")
    }

    /** 球形画三个大圆，圆柱画上下圆与母线，高度范围与半高一致。 */
    @Test
    fun shouldDrawSphereArcsAndCylinderCaps() {
        val world = Mockito.mock(World::class.java)
        val center = Location(world, 0.0, 64.0, 0.0)

        val spherePoints = SelectorDebug.rangePoints(
            world,
            RangeNearestEntityFinder(center, 5.0, null, emptyList()),
            0.5,
        )
        assertTrue(spherePoints.isNotEmpty(), "球形必须有采样点")
        for (point in spherePoints) {
            val deltaX = point.x - center.x
            val deltaY = point.y - center.y
            val deltaZ = point.z - center.z
            val distance = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
            assertEquals(5.0, distance, 1.0e-9, "球形调试点必须落在球面上")
        }

        var sawBottomCap = false
        var sawTopCap = false
        val cylinderPoints = SelectorDebug.rangePoints(
            world,
            RangeNearestEntityFinder(center, 5.0, 2.0, emptyList()),
            0.5,
        )
        assertTrue(cylinderPoints.isNotEmpty(), "圆柱必须有采样点")
        for (point in cylinderPoints) {
            val deltaX = point.x - center.x
            val deltaZ = point.z - center.z
            assertTrue(sqrt(deltaX * deltaX + deltaZ * deltaZ) <= 5.000001, "圆柱调试点不能超出半径")
            assertTrue(point.y >= 61.999999 && point.y <= 66.000001, "圆柱调试点不能超出台面高度")
            if (abs(point.y - 62.0) < 1.0e-9) {
                sawBottomCap = true
            }
            if (abs(point.y - 66.0) < 1.0e-9) {
                sawTopCap = true
            }
        }
        assertTrue(sawBottomCap, "圆柱必须画出下底圆")
        assertTrue(sawTopCap, "圆柱必须画出上底圆")
    }

    /** 采样间距写错时要在应用配置阶段报错，不能带着错误参数绘制。 */
    @Test
    fun shouldRejectNonPositiveStep() {
        assertThrows<IllegalArgumentException> {
            SelectorDebug.apply(true, Particle.END_ROD, 0.0, 0.15)
        }
    }

    /**
     * 构造与贤者惩戒光束一致的矩形查找器。
     *
     * @param world 世界替身。
     * @return 矩形查找器。
     */
    private fun rectFinder(world: World): RectNearestEntityFinder {
        return RectNearestEntityFinder(
            Location(world, 10.0, 20.0, 30.0),
            1.5,
            2.4,
            11.0,
            0.0f,
            0.0,
            1.0,
            7.5,
            emptyList(),
        )
    }

    /**
     * 构造半径 10 格、开角 90 度、朝向 30 度的扇形查找器。
     *
     * @param world 世界替身。
     * @return 扇形查找器。
     */
    private fun sectorFinder(world: World): SectorNearestEntityFinder {
        return SectorNearestEntityFinder(
            Location(world, 5.0, 64.0, -3.0, 30.0f, 0.0f),
            90.0,
            10.0,
            30.0f,
            emptyList(),
        )
    }

    /**
     * 构造玩家替身，粒子调用会被 Mockito 记录。
     *
     * @return 玩家替身。
     */
    private fun player(): Player {
        return Mockito.mock(Player::class.java)
    }

    /**
     * 统计发送过的粒子点数。
     *
     * @param player 玩家替身。
     * @return spawnParticle 调用次数。
     */
    private fun particleCount(player: Player): Int {
        return Mockito.mockingDetails(player).invocations.count { invocation ->
            invocation.method.name == "spawnParticle"
        }
    }

    /**
     * 取第一次发送的粒子类型。
     *
     * @param player 玩家替身。
     * @return 粒子类型；没有调用过时返回 null。
     */
    private fun firstParticle(player: Player): Any? {
        for (invocation in Mockito.mockingDetails(player).invocations) {
            if (invocation.method.name == "spawnParticle") {
                return invocation.arguments[0]
            }
        }
        return null
    }
}
