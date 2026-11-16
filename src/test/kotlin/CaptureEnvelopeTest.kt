import com.gitee.planners.module.action.CaptureEnvelope
import org.bukkit.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 会话捕捉的包络几何：位移必须投影到朝向轴后放大盒体，静止时退化为原尺寸。
 */
class CaptureEnvelopeTest {

    /** 静止不动时尺寸与偏移保持不变。 */
    @Test
    fun shouldKeepDimensionsWhenStandingStill() {
        val previous = Location(null, 0.0, 0.0, 0.0)
        val current = Location(null, 0.0, 0.0, 0.0)
        val finder = CaptureEnvelope.finderFor(
            previous,
            current,
            0.0f,
            doubleArrayOf(1.6, 2.0, 1.4),
            doubleArrayOf(0.0, 0.8, 1.0),
            doubleArrayOf(0.0, 0.8, 1.0),
            emptyList(),
        )

        assertEquals(1.6, finder.width, 1.0e-9)
        assertEquals(2.0, finder.height, 1.0e-9)
        assertEquals(1.4, finder.length, 1.0e-9)
        assertEquals(1.0, finder.centerZ, 1.0e-9)
    }

    /** 朝正前方移动 1 格时只在长度方向增长。 */
    @Test
    fun shouldGrowLengthWhenMovingForward() {
        val finder = CaptureEnvelope.finderFor(
            Location(null, 0.0, 0.0, 0.0),
            Location(null, 0.0, 0.0, 1.0),
            0.0f,
            doubleArrayOf(1.6, 2.0, 1.4),
            doubleArrayOf(0.0, 0.8, 0.0),
            doubleArrayOf(0.0, 0.8, 0.0),
            emptyList(),
        )

        assertEquals(1.6, finder.width, 1.0e-9)
        assertEquals(1.4 + 1.0, finder.length, 1.0e-9)
        assertEquals(0.5, finder.centerZ, 1.0e-9)
    }

    /** 侧向移动只在宽度方向增长。 */
    @Test
    fun shouldGrowWidthWhenMovingSideways() {
        val finder = CaptureEnvelope.finderFor(
            Location(null, 0.0, 0.0, 0.0),
            Location(null, 1.0, 0.0, 0.0),
            0.0f,
            doubleArrayOf(1.6, 2.0, 1.4),
            doubleArrayOf(0.0, 0.8, 0.0),
            doubleArrayOf(0.0, 0.8, 0.0),
            emptyList(),
        )

        assertEquals(1.6 + 1.0, finder.width, 1.0e-9)
        assertEquals(1.4, finder.length, 1.0e-9)
    }

    /** 朝向 90 度时，世界 Z 方向位移被算成侧向偏移。 */
    @Test
    fun shouldRotateMovementWithFacing() {
        val finder = CaptureEnvelope.finderFor(
            Location(null, 0.0, 0.0, 0.0),
            Location(null, 0.0, 0.0, 1.0),
            90.0f,
            doubleArrayOf(1.6, 2.0, 1.4),
            doubleArrayOf(0.0, 0.8, 0.0),
            doubleArrayOf(0.0, 0.8, 0.0),
            emptyList(),
        )

        assertEquals(1.6 + 1.0, finder.width, 1.0e-9)
        assertEquals(1.4, finder.length, 1.0e-9)
    }

    /** 前后偏移变化计入长度与中心偏移。 */
    @Test
    fun shouldAccountForOffsetChange() {
        val finder = CaptureEnvelope.finderFor(
            Location(null, 0.0, 0.0, 0.0),
            Location(null, 0.0, 0.0, 0.0),
            0.0f,
            doubleArrayOf(1.6, 2.0, 1.4),
            doubleArrayOf(0.0, 0.8, 1.0),
            doubleArrayOf(0.0, 0.8, 1.5),
            emptyList(),
        )

        assertEquals(1.4 + 0.5, finder.length, 1.0e-9)
        assertEquals(1.25, finder.centerZ, 1.0e-9)
    }
}
