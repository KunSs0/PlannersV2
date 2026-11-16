import com.gitee.planners.api.common.util.NearestEntityFinder
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 选区必须使用实体真实 AABB，并且跟随实时位置，不能跨查询复用旧坐标。
 */
class NearestEntityFinderBoundingBoxTest {

    /**
     * 碰撞箱应原样取自实体，不能再用工位与宽高拼装出双倍尺寸的盒子。
     */
    @Test
    fun shouldUseEntityBoundingBox() {
        val box = BoundingBox(9.7, 64.0, 19.7, 10.3, 65.8, 20.3)
        val entity = entityWith(box)
        val finder = probeFinder(listOf(entity))

        val actual = finder.getBoundingBox(entity)

        assertEquals(9.7, actual.minX, 1.0e-9)
        assertEquals(10.3, actual.maxX, 1.0e-9)
        assertEquals(64.0, actual.minY, 1.0e-9)
        assertEquals(65.8, actual.maxY, 1.0e-9)
        assertEquals(19.7, actual.minZ, 1.0e-9)
        assertEquals(20.3, actual.maxZ, 1.0e-9)
    }

    /**
     * 实体移动后再次查询，应返回新位置对应的包围盒。
     */
    @Test
    fun shouldRefreshBoundingBoxAfterEntityMoves() {
        val box = BoundingBox(9.7, 64.0, 19.7, 10.3, 65.8, 20.3)
        val entity = entityWith(box)
        val finder = probeFinder(listOf(entity))

        assertEquals(9.7, finder.getBoundingBox(entity).minX, 1.0e-9)

        box.shift(20.0, 0.0, 0.0)

        assertEquals(29.7, finder.getBoundingBox(entity).minX, 1.0e-9)
        assertEquals(30.3, finder.getBoundingBox(entity).maxX, 1.0e-9)
    }

    /**
     * 实体离开扇形后，下一次选区不应再命中它。
     */
    @Test
    fun shouldNotSelectEntityAfterItLeavesSector() {
        val box = BoundingBox(-0.3, 64.0, 4.7, 0.3, 65.8, 5.3)
        val entity = entityWith(box)
        val finder = SectorNearestEntityFinder(
            Location(null, 0.0, 64.0, 0.0),
            60.0,
            10.0,
            0.0f,
            listOf(entity),
        )

        assertEquals(listOf(entity), finder.request())

        box.shift(5.0, 0.0, -5.0)

        assertTrue(finder.request().isEmpty())
    }

    /**
     * 构造只用于读取包围盒的查找器替身。
     *
     * @param entities 候选实体。
     * @return 查找器。
     */
    private fun probeFinder(entities: List<Entity>): NearestEntityFinder {
        return object : NearestEntityFinder(Location(null, 0.0, 64.0, 0.0), entities) {
            override fun request(): List<Entity> = samples

            override fun samplingBounds(): BoundingBox = BoundingBox(-1.0, 63.0, -1.0, 1.0, 65.0, 1.0)
        }
    }

    /**
     * 构造带固定碰撞箱的实体替身。
     *
     * @param box 碰撞箱。
     * @return 实体替身。
     */
    private fun entityWith(box: BoundingBox): Entity {
        val entity = Mockito.mock(Entity::class.java)
        Mockito.`when`(entity.boundingBox).thenReturn(box)
        return entity
    }
}
