import com.gitee.planners.api.common.util.SelectionGeometry
import org.bukkit.util.BoundingBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI

/**
 * 选区几何的纯计算用例：球形与圆柱选区按碰撞箱判定，不再只看实体位置点。
 */
class SelectionGeometryTest {

    /**
     * 碰撞箱压住球面时命中，完全在球外时不命中。
     */
    @Test
    fun shouldUseBoundingBoxForSphere() {
        val touching = BoundingBox(2.8, -0.5, -0.5, 3.2, 0.5, 0.5)
        val outside = BoundingBox(3.1, -0.5, -0.5, 3.5, 0.5, 0.5)

        assertTrue(SelectionGeometry.sphereTouchesBox(0.0, 0.0, 0.0, 3.0, touching))
        assertFalse(SelectionGeometry.sphereTouchesBox(0.0, 0.0, 0.0, 3.0, outside))
    }

    /**
     * 圆柱选区同时受水平半径与垂直半高约束。
     */
    @Test
    fun shouldApplyBothCylinderLimits() {
        val reachable = BoundingBox(0.5, 0.9, -0.5, 1.5, 2.9, 0.5)
        val tooHigh = BoundingBox(0.5, 1.2, -0.5, 1.5, 3.2, 0.5)
        val tooFar = BoundingBox(3.4, -0.2, -0.2, 3.8, 0.2, 0.2)

        assertTrue(SelectionGeometry.cylinderTouchesBox(0.0, 0.0, 0.0, 3.0, 1.0, reachable))
        assertFalse(SelectionGeometry.cylinderTouchesBox(0.0, 0.0, 0.0, 3.0, 1.0, tooHigh))
        assertFalse(SelectionGeometry.cylinderTouchesBox(0.0, 0.0, 0.0, 3.0, 1.0, tooFar))
    }

    /**
     * 整圈扇形退化为圆，只比半径。
     */
    @Test
    fun shouldTreatFullCircleSectorAsDisc() {
        val reachable = BoundingBox(-0.5, 0.0, -3.4, 0.5, 1.0, -2.6)
        val beyond = BoundingBox(-0.5, 0.0, -4.2, 0.5, 1.0, -3.8)

        assertTrue(SelectionGeometry.sectorTouchesBox(0.0, 0.0, 3.5, PI, 0.0, 1.0, reachable))
        assertFalse(SelectionGeometry.sectorTouchesBox(0.0, 0.0, 3.5, PI, 0.0, 1.0, beyond))
    }

    /**
     * 取样包围盒必须覆盖整个形状。
     */
    @Test
    fun shouldCoverShapeWithSamplingBounds() {
        val sphere = SelectionGeometry.sphereBounds(1.0, 2.0, 3.0, 2.5)
        val cylinder = SelectionGeometry.cylinderBounds(1.0, 2.0, 3.0, 2.5, 1.5)

        assertEquals(-1.5, sphere.minX, 1.0e-9)
        assertEquals(3.5, sphere.maxX, 1.0e-9)
        assertEquals(-0.5, sphere.minY, 1.0e-9)
        assertEquals(4.5, sphere.maxY, 1.0e-9)
        assertEquals(0.5, cylinder.minY, 1.0e-9)
        assertEquals(3.5, cylinder.maxY, 1.0e-9)
    }

    /**
     * 扇形判定不能漏掉网格取样能找到的交集点（反向一致性检查）。
     */
    @Test
    fun shouldNotMissSectorIntersectionFoundByGridSampling() {
        val halfAngle = PI / 3.0
        val radius = 4.0
        var seed = 20261116L
        for (case in 0 until 120) {
            seed = nextSeed(seed)
            val centerX = ((seed ushr 24) % 701).toDouble() / 100.0 - 3.5
            seed = nextSeed(seed)
            val centerZ = ((seed ushr 24) % 701).toDouble() / 100.0 - 3.5
            seed = nextSeed(seed)
            val halfWidth = ((seed ushr 24) % 90).toDouble() / 100.0 + 0.05
            seed = nextSeed(seed)
            val halfDepth = ((seed ushr 24) % 90).toDouble() / 100.0 + 0.05
            val box = BoundingBox(centerX - halfWidth, 0.0, centerZ - halfDepth, centerX + halfWidth, 1.0, centerZ + halfDepth)
            val touched = SelectionGeometry.sectorTouchesBox(0.0, 0.0, radius, halfAngle, 0.0, 1.0, box)
            if (!touched) {
                assertFalse(
                    gridFindsSectorPoint(box, radius, halfAngle),
                    "网格取样找到扇形交集却被判为不相交：case=$case box=$box",
                )
            }
        }
    }

    /**
     * 旋转矩形判定不能漏掉网格取样能找到的交集点（反向一致性检查）。
     */
    @Test
    fun shouldNotMissRotatedBoxIntersectionFoundByGridSampling() {
        val halfWidth = 0.8
        val halfLength = 5.5
        var seed = 771026L
        for (case in 0 until 120) {
            seed = nextSeed(seed)
            val centerX = ((seed ushr 24) % 701).toDouble() / 100.0 - 3.5
            seed = nextSeed(seed)
            val centerZ = ((seed ushr 24) % 701).toDouble() / 100.0 - 3.5
            seed = nextSeed(seed)
            val yaw = ((seed ushr 24) % 628).toDouble() / 100.0
            val forwardX = -kotlin.math.sin(yaw)
            val forwardZ = kotlin.math.cos(yaw)
            val rightX = kotlin.math.cos(yaw)
            val rightZ = kotlin.math.sin(yaw)
            seed = nextSeed(seed)
            val halfWidthBox = ((seed ushr 24) % 90).toDouble() / 100.0 + 0.05
            seed = nextSeed(seed)
            val halfDepthBox = ((seed ushr 24) % 90).toDouble() / 100.0 + 0.05
            val box = BoundingBox(centerX - halfWidthBox, 0.0, centerZ - halfDepthBox, centerX + halfWidthBox, 1.0, centerZ + halfDepthBox)
            val touched = SelectionGeometry.rotatedBoxTouchesBox(
                centerX = 0.0,
                centerY = 0.5,
                centerZ = 0.0,
                halfWidth = halfWidth,
                halfHeight = 1.5,
                halfLength = halfLength,
                forwardX = forwardX,
                forwardZ = forwardZ,
                rightX = rightX,
                rightZ = rightZ,
                box = box,
            )
            if (!touched) {
                assertFalse(
                    gridFindsRotatedBoxPoint(box, forwardX, forwardZ, rightX, rightZ, halfWidth, halfLength),
                    "网格取样找到矩形交集却被判为不相交：case=$case box=$box",
                )
            }
        }
    }

    /**
     * 网格取样检查扇形与碰撞箱是否有公共点。
     *
     * @param box 碰撞箱。
     * @param radius 半径。
     * @param halfAngle 半开角。
     * @return 找到一个同时属于两者的点时返回 true。
     */
    private fun gridFindsSectorPoint(box: BoundingBox, radius: Double, halfAngle: Double): Boolean {
        val steps = 32
        for (indexX in 0..steps) {
            for (indexZ in 0..steps) {
                val pointX = box.minX + (box.maxX - box.minX) * indexX / steps
                val pointZ = box.minZ + (box.maxZ - box.minZ) * indexZ / steps
                if (pointX * pointX + pointZ * pointZ > radius * radius) {
                    continue
                }
                // 中轴为 +Z，用 atan2(x, z) 求与中轴的夹角。
                if (kotlin.math.abs(kotlin.math.atan2(pointX, pointZ)) <= halfAngle) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 网格取样检查旋转矩形与碰撞箱是否有公共点。
     *
     * @param box 碰撞箱。
     * @param forwardX 前方单位向量 X 分量。
     * @param forwardZ 前方单位向量 Z 分量。
     * @param rightX 右方单位向量 X 分量。
     * @param rightZ 右方单位向量 Z 分量。
     * @param halfWidth 矩形半宽。
     * @param halfLength 矩形半长。
     * @return 找到一个同时属于两者的点时返回 true。
     */
    private fun gridFindsRotatedBoxPoint(
        box: BoundingBox,
        forwardX: Double,
        forwardZ: Double,
        rightX: Double,
        rightZ: Double,
        halfWidth: Double,
        halfLength: Double,
    ): Boolean {
        val steps = 32
        for (indexX in 0..steps) {
            for (indexZ in 0..steps) {
                val pointX = box.minX + (box.maxX - box.minX) * indexX / steps
                val pointZ = box.minZ + (box.maxZ - box.minZ) * indexZ / steps
                val localX = pointX * rightX + pointZ * rightZ
                val localZ = pointX * forwardX + pointZ * forwardZ
                if (kotlin.math.abs(localX) > halfWidth) {
                    continue
                }
                if (kotlin.math.abs(localZ) > halfLength) {
                    continue
                }
                return true
            }
        }
        return false
    }

    /**
     * 线性同余伪随机，保证用例可重复。
     *
     * @param seed 当前种子。
     * @return 下一个种子。
     */
    private fun nextSeed(seed: Long): Long {
        return seed * 6364136223846793005L + 1442695040888963407L
    }
}
