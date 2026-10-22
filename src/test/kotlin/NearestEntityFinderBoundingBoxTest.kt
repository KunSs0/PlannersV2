import com.gitee.planners.api.common.util.NearestEntityFinder
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 选区碰撞箱必须跟随实体实时位置，不能跨查询复用旧坐标。
 */
class NearestEntityFinderBoundingBoxTest {

    /**
     * 实体移动后再次查询，应返回新位置对应的包围盒。
     */
    @Test
    fun shouldRefreshBoundingBoxAfterEntityMoves() {
        val location = Location(null, 10.0, 64.0, 20.0)
        val entity = entityAt(location)
        val finder = object : NearestEntityFinder(
            Location(null, 0.0, 64.0, 0.0),
            listOf(entity)
        ) {
            override fun request(): List<Entity> = samples
        }

        val before = finder.getBoundingBox(entity)
        assertEquals(9.5, before.minX, 1.0e-9)
        assertEquals(1.0, before.maxX - before.minX, 1.0e-9)

        location.x = 30.0

        val after = finder.getBoundingBox(entity)
        assertEquals(29.5, after.minX, 1.0e-9)
        assertEquals(30.5, after.maxX, 1.0e-9)
    }

    /**
     * 实体从扇形内移动到扇形外后，下一次选区不应再命中它。
     */
    @Test
    fun shouldNotSelectEntityAfterItLeavesSector() {
        val location = Location(null, 0.0, 64.0, 5.0)
        val entity = entityAt(location)
        val finder = SectorNearestEntityFinder(
            Location(null, 0.0, 64.0, 0.0),
            60.0,
            10.0,
            0.0f,
            listOf(entity)
        )

        assertEquals(listOf(entity), finder.request())

        location.x = 5.0
        location.z = 0.0

        assertTrue(finder.request().isEmpty())
    }

    /**
     * 创建带固定尺寸、可移动位置的实体替身。
     */
    private fun entityAt(location: Location): Entity {
        val entity = Mockito.mock(Entity::class.java)
        Mockito.`when`(entity.width).thenReturn(0.5)
        Mockito.`when`(entity.height).thenReturn(1.0)
        Mockito.`when`(entity.location).thenReturn(location)
        return entity
    }
}
