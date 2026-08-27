import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** 验证 Nova 通过显式绑定访问 Bukkit/Planners 风格宿主对象的技能树场景。 */
class SkillTreeBukkitPlannersAccessScenarioTest {

    @TempDir
    lateinit var root: Path

    /** 验证路由、技能树与节点数据从宿主对象进入 Nova 快照。 */
    @Test
    fun measureBukkitAndPlannersObjectAccessFlow() {
        val workspace = createWorkspace(root.resolve("object-flow"))
        try {
            val route = MockPlayerRoute("knight_warder", 2, 42)
            val payload = workspace.invoke(MODULE, "snapshot", mapOf("route" to route)).toString()
            assertTrue(payload.isNotEmpty())
            assertTrue(payload.contains("knight_warder"))
            assertTrue(payload.contains("nodes=42"))
        } finally {
            workspace.close()
        }
    }

    /** 分阶段调用同一预编译模块并确认宿主访问结果一致。 */
    @Test
    fun breakdownSnapshotStages() {
        val workspace = createWorkspace(root.resolve("stages"))
        try {
            val route = MockPlayerRoute("knight_warder", 2, 42)
            val routeId = workspace.invoke(MODULE, "routeId", mapOf("route" to route))
            val treeCount = workspace.invoke(MODULE, "treeCount", mapOf("route" to route))
            val nodeCount = workspace.invoke(MODULE, "nodeCount", mapOf("route" to route))
            assertEquals("knight_warder", routeId)
            assertEquals(2L, (treeCount as Number).toLong())
            assertEquals(42L, (nodeCount as Number).toLong())
        } finally {
            workspace.close()
        }
    }

    /** 验证节点集合由 Nova 遍历，且模块源码不包含动态宿主类加载。 */
    @Test
    fun breakdownTreeNodeData() {
        val workspace = createWorkspace(root.resolve("nodes"))
        try {
            val nodes = ArrayList<Map<String, Any>>()
            for (index in 1..42) {
                nodes.add(mapOf("id" to "node-$index", "level" to index))
            }
            val result = workspace.invoke(MODULE, "sumLevels", emptyMap(), nodes)
            assertEquals(903L, (result as Number).toLong())
            assertTrue(!SOURCE.contains("Java.type"))
        } finally {
            workspace.close()
        }
    }

    /** 创建并预编译宿主对象访问业务模块。 */
    private fun createWorkspace(path: Path): NovaScenarioWorkspace {
        val workspace = NovaScenarioWorkspace(path)
        workspace.register(MODULE, SOURCE)
        workspace.load()
        return workspace
    }

    /** 提供给 Nova 的最小玩家路由宿主对象。 */
    class MockPlayerRoute(
        private val routeId: String,
        private val treeCount: Int,
        private val nodeCount: Int
    ) {

        /** @return 路由业务标识。 */
        fun getRouteId(): String {
            return routeId
        }

        /** @return 技能树数量。 */
        fun getTreeCount(): Int {
            return treeCount
        }

        /** @return 节点数量。 */
        fun getNodeCount(): Int {
            return nodeCount
        }
    }

    companion object {
        private const val MODULE = "@planners/generated/bukkit-access"
        private const val SOURCE =
            "fun routeId() = route.getRouteId()\n" +
                "fun treeCount() = route.getTreeCount()\n" +
                "fun nodeCount() = route.getNodeCount()\n" +
                "fun snapshot() = route.getRouteId() + \" trees=\" + route.getTreeCount() + \" nodes=\" + route.getNodeCount()\n" +
                "fun sumLevels(nodes) {\n" +
                "  var total = 0\n" +
                "  for (node in nodes) { total += node.get(\"level\") }\n" +
                "  return total\n" +
                "}\n"
    }
}
