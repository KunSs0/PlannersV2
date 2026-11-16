import com.gitee.planners.api.common.util.RectNearestEntityFinder
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 旋转矩形选区必须按实体真实 AABB 判定：身体擦过矩形边缘要命中，完全分离则不能命中。
 */
class RectNearestEntityFinderTest {

    /**
     * 身体与矩形侧向重叠时应命中，包括中心仍在矩形外的情况。
     */
    @Test
    fun shouldHitEntityTouchingSideEdge() {
        val inside = entityAt(0.5, 0.0, 5.0, 1.0, 1.0)
        val touching = entityAt(0.9, 0.0, 5.0, 1.0, 1.0)
        val separated = entityAt(1.3, 0.0, 5.0, 1.0, 1.0)

        assertTrue(finderOf(listOf(inside)).request().contains(inside))
        assertTrue(finderOf(listOf(touching)).request().contains(touching))
        assertTrue(finderOf(listOf(separated)).request().isEmpty())
    }

    /**
     * 矩形前后端点同样按碰撞箱判定，不再要求实体中心进入矩形。
     */
    @Test
    fun shouldHitEntityTouchingFrontAndBackEdge() {
        val nearTouching = entityAt(0.0, 0.0, 1.6, 1.0, 1.0)
        val nearSeparated = entityAt(0.0, 0.0, 1.3, 1.0, 1.0)
        val farTouching = entityAt(0.0, 0.0, 13.4, 1.0, 1.0)
        val farSeparated = entityAt(0.0, 0.0, 13.6, 1.0, 1.0)

        assertTrue(finderOf(listOf(nearTouching)).request().contains(nearTouching))
        assertTrue(finderOf(listOf(nearSeparated)).request().isEmpty())
        assertTrue(finderOf(listOf(farTouching)).request().contains(farTouching))
        assertTrue(finderOf(listOf(farSeparated)).request().isEmpty())
    }

    /**
     * 脚底位于矩形下方但身体伸入矩形时，高大体型应命中。
     */
    @Test
    fun shouldHitTallEntityReachingIntoTall() {
        val tall = entityAt(0.0, -1.0, 5.0, 1.0, 3.5)
        val below = entityAt(0.0, -4.0, 5.0, 1.0, 1.0)

        assertTrue(finderOf(listOf(tall)).request().contains(tall))
        assertTrue(finderOf(listOf(below)).request().isEmpty())
    }

    /**
     * 旋转后仍使用同一套 AABB 判定，侧向边界随朝向旋转。
     */
    @Test
    fun shouldRotateJudgementWithFacing() {
        val touching = entityAt(-7.5, 0.0, 0.6, 1.0, 1.0)
        val separated = entityAt(-7.5, 0.0, 1.3, 1.0, 1.0)

        assertTrue(findRect(listOf(touching), 90.0f).request().contains(touching))
        assertTrue(findRect(listOf(separated), 90.0f).request().isEmpty())
    }

    /**
     * 取样包围盒必须完整覆盖旋转矩形，否则会漏掉贴边实体。
     */
    @Test
    fun shouldComputeSamplingBoundsCoveringRect() {
        val bounds = finderOf(emptyList()).samplingBounds()

        assertEquals(-0.75, bounds.minX, 1.0e-9)
        assertEquals(0.75, bounds.maxX, 1.0e-9)
        assertEquals(-0.2, bounds.minY, 1.0e-9)
        assertEquals(2.2, bounds.maxY, 1.0e-9)
        assertEquals(2.0, bounds.minZ, 1.0e-9)
        assertEquals(13.0, bounds.maxZ, 1.0e-9)
    }

    /**
     * 同一查找器换候选列表后，结果应跟随新列表。
     */
    @Test
    fun shouldRefreshSamples() {
        val first = entityAt(0.5, 0.0, 5.0, 1.0, 1.0)
        val second = entityAt(0.9, 0.0, 5.0, 1.0, 1.0)
        val finder = finderOf(listOf(first))

        assertEquals(listOf(first), finder.request())

        finder.samples = listOf(second)

        assertEquals(listOf(second), finder.request())
    }

    /**
     * 构造与贤者惩戒光束一致的矩形查找器。
     *
     * @param entities 候选实体。
     * @return 查找器。
     */
    private fun finderOf(entities: List<Entity>): RectNearestEntityFinder {
        return findRect(entities, 0.0f)
    }

    /**
     * 构造指定朝向的矩形查找器。
     *
     * @param entities 候选实体。
     * @param yaw 朝向。
     * @return 查找器。
     */
    private fun findRect(entities: List<Entity>, yaw: Float): RectNearestEntityFinder {
        return RectNearestEntityFinder(
            Location(null, 0.0, 0.0, 0.0),
            1.5,
            2.4,
            11.0,
            yaw,
            0.0,
            1.0,
            7.5,
            entities,
        )
    }

    /**
     * 构造带真实 AABB 的实体替身。
     *
     * @param x 位置 X。
     * @param feetY 脚底 Y。
     * @param z 位置 Z。
     * @param width 宽度。
     * @param height 高度。
     * @return 实体替身。
     */
    private fun entityAt(x: Double, feetY: Double, z: Double, width: Double, height: Double): Entity {
        val half = width / 2.0
        val box = BoundingBox(x - half, feetY, z - half, x + half, feetY + height, z + half)
        val entity = Mockito.mock(Entity::class.java)
        Mockito.`when`(entity.boundingBox).thenReturn(box)
        return entity
    }
}
