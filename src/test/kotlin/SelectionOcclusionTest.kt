import com.gitee.planners.api.common.util.SelectionOcclusion
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.BoundingBox
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector
import org.bukkit.util.VoxelShape
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 选区遮挡判定：白名单方块逐块放行，非白名单实心方块立即挡住视线。
 */
class SelectionOcclusionTest {

    /** 每个用例前清空白名单。 */
    @BeforeEach
    fun reset() {
        SelectionOcclusion.configure(emptyList())
    }

    /** 射线没有任何方块命中时视线畅通。 */
    @Test
    fun shouldSeeThroughEmptySpace() {
        val world = worldReturning(null)

        assertTrue(SelectionOcclusion.isClear(world, Location(world, 0.0, 1.0, 0.0), targetBox()))
    }

    /** 石墙挡住视线。 */
    @Test
    fun shouldBlockOnWall() {
        val wall = block(Material.STONE_BRICKS, BoundingBox(0.0, 0.0, 2.0, 1.0, 4.0, 3.0))
        val world = worldReturning(hit(wall))

        assertFalse(SelectionOcclusion.isClear(world, Location(world, 0.0, 1.0, 0.0), targetBox()))
    }

    /** 白名单栅栏不遮挡，射线从其碰撞形状出口继续前进。 */
    @Test
    fun shouldPassThroughWhitelistedFence() {
        SelectionOcclusion.configure(listOf(Material.OAK_FENCE))
        val fence = block(Material.OAK_FENCE, BoundingBox(0.0, 0.0, 2.0, 1.0, 4.0, 3.0))
        val world = worldReturning(hit(fence), null)

        assertTrue(SelectionOcclusion.isClear(world, Location(world, 0.0, 1.0, 0.0), targetBox()))
    }

    /** 连续多道栅栏同样通过（与 `minecraft:fences` 标签一致：木质栅栏与下界砖栅栏都放行）。 */
    @Test
    fun shouldPassThroughSeveralFences() {
        SelectionOcclusion.configure(listOf(Material.OAK_FENCE, Material.NETHER_BRICK_FENCE))
        val first = block(Material.OAK_FENCE, BoundingBox(0.0, 0.0, 2.0, 1.0, 4.0, 3.0))
        val second = block(Material.NETHER_BRICK_FENCE, BoundingBox(0.0, 0.0, 3.0, 1.0, 4.0, 4.0))
        val world = worldReturning(hit(first), hit(second), null)

        assertTrue(SelectionOcclusion.isClear(world, Location(world, 0.0, 1.0, 0.0), targetBox()))
    }

    /** 栅栏后还有石墙时仍被遮挡。 */
    @Test
    fun shouldBlockOnWallBehindFence() {
        SelectionOcclusion.configure(listOf(Material.OAK_FENCE))
        val fence = block(Material.OAK_FENCE, BoundingBox(0.0, 0.0, 2.0, 1.0, 4.0, 3.0))
        val wall = block(Material.STONE_BRICKS, BoundingBox(0.0, 0.0, 3.0, 1.0, 4.0, 4.0))
        val world = worldReturning(hit(fence), hit(wall))

        assertFalse(SelectionOcclusion.isClear(world, Location(world, 0.0, 1.0, 0.0), targetBox()))
    }

    /** 目标碰撞箱包住起点时视为没有遮挡。 */
    @Test
    fun shouldTreatOverlappingStartAsClear() {
        val world = Mockito.mock(World::class.java)

        assertTrue(
            SelectionOcclusion.isClear(
                world,
                Location(world, 0.5, 1.0, 2.0),
                BoundingBox(0.0, 0.0, 0.0, 1.0, 2.0, 3.0),
            ),
        )
    }

    /**
     * 构造目标碰撞箱：正前方 4~5 格。
     *
     * @return 碰撞箱。
     */
    private fun targetBox(): BoundingBox {
        return BoundingBox(-0.3, 0.0, 4.0, 0.3, 1.8, 4.6)
    }

    /**
     * 构造按顺序返回命中结果的替身世界。
     *
     * @param results 依次返回的命中结果。
     * @return 世界替身。
     */
    private fun worldReturning(vararg results: RayTraceResult?): World {
        val world = Mockito.mock(World::class.java)
        Mockito.`when`(
            world.rayTraceBlocks(
                Mockito.any(Location::class.java),
                Mockito.any(Vector::class.java),
                Mockito.anyDouble(),
                Mockito.any(FluidCollisionMode::class.java),
                Mockito.anyBoolean(),
            ),
        ).thenReturn(results.first(), *results.drop(1).toTypedArray())
        return world
    }

    /**
     * 构造方块替身。
     *
     * @param type 方块类型。
     * @param shape 碰撞形状外框。
     * @return 方块替身。
     */
    private fun block(type: Material, shape: BoundingBox): Block {
        val block = Mockito.mock(Block::class.java)
        Mockito.`when`(block.type).thenReturn(type)
        val voxel = Mockito.mock(VoxelShape::class.java)
        Mockito.`when`(voxel.boundingBoxes).thenReturn(listOf(shape))
        Mockito.`when`(block.collisionShape).thenReturn(voxel)
        return block
    }

    /**
     * 构造命中结果替身。
     *
     * @param block 命中的方块。
     * @return 命中结果替身。
     */
    private fun hit(block: Block): RayTraceResult {
        val result = Mockito.mock(RayTraceResult::class.java)
        Mockito.`when`(result.hitBlock).thenReturn(block)
        return result
    }
}
