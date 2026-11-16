import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 扇形选区必须按实体真实 AABB 判定：压住扇面或圆弧边缘要命中，完全分离则不能命中。
 */
class SectorNearestEntityFinderTest {

    /**
     * 正前方与圆弧边缘压线的实体都应命中。
     */
    @Test
    fun shouldHitEntityTouchingArc() {
        val ahead = entityAt(0.0, 0.0, 2.8, 0.5)
        val touchingArc = entityAt(0.0, 0.0, 3.65, 0.5)
        val beyondArc = entityAt(0.0, 0.0, 3.9, 0.5)

        assertTrue(finderOf(listOf(ahead), 70.0).request().contains(ahead))
        assertTrue(finderOf(listOf(touchingArc), 70.0).request().contains(touchingArc))
        assertTrue(finderOf(listOf(beyondArc), 70.0).request().isEmpty())
    }

    /**
     * 中心在开角外、身体跨过扇面边界的宽体应命中。
     */
    @Test
    fun shouldHitWideEntityCrossingWedgeEdge() {
        val crossing = entityAt(2.6, 0.0, 2.4, 2.0)
        val outside = entityAt(3.2, 0.0, 2.0, 0.5)

        assertTrue(finderOf(listOf(crossing), 70.0).request().contains(crossing))
        assertTrue(finderOf(listOf(outside), 70.0).request().isEmpty())
    }

    /**
     * 扇形背后的实体不命中；整圈扇形则命中。
     */
    @Test
    fun shouldOnlyHitEntityInsideAngle() {
        val behind = entityAt(0.0, 0.0, -2.0, 0.5)

        assertTrue(finderOf(listOf(behind), 70.0).request().isEmpty())
        assertTrue(finderOf(listOf(behind), 360.0).request().contains(behind))
    }

    /**
     * 取样包围盒必须覆盖整个扇形半径。
     */
    @Test
    fun shouldComputeSamplingBoundsCoveringSector() {
        val bounds = finderOf(emptyList(), 70.0).samplingBounds()

        assertEquals(-3.5, bounds.minX, 1.0e-9)
        assertEquals(3.5, bounds.maxX, 1.0e-9)
        assertEquals(3.5, bounds.maxZ, 1.0e-9)
    }

    /**
     * 构造扇形查找器。
     *
     * @param entities 候选实体。
     * @param angle 开角。
     * @return 查找器。
     */
    private fun finderOf(entities: List<Entity>, angle: Double): SectorNearestEntityFinder {
        return SectorNearestEntityFinder(Location(null, 0.0, 0.0, 0.0), angle, 3.5, 0.0f, entities)
    }

    /**
     * 构造带真实 AABB 的实体替身。
     *
     * @param x 位置 X。
     * @param feetY 脚底 Y。
     * @param z 位置 Z。
     * @param width 宽度。
     * @return 实体替身。
     */
    private fun entityAt(x: Double, feetY: Double, z: Double, width: Double): Entity {
        val half = width / 2.0
        val box = BoundingBox(x - half, feetY, z - half, x + half, feetY + 1.0, z + half)
        val entity = Mockito.mock(Entity::class.java)
        Mockito.`when`(entity.boundingBox).thenReturn(box)
        return entity
    }
}
