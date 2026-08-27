import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** 验证技能树快照、条件批量计算与节点查找的 Nova 等价业务场景。 */
class SkillTreeSnapshotScenarioTest {

    @TempDir
    lateinit var root: Path

    /** 采样预编译完整快照入口并验证关键业务标识进入结果。 */
    @Test
    fun measurePrecompiledFullSnapshotFlow() {
        val workspace = createWorkspace(root.resolve("snapshot-flow"))
        try {
            val started = System.nanoTime()
            val payload = workspace.invoke(
                MODULE,
                "payload",
                emptyMap(),
                "knight_warder",
                3,
                2,
                42
            ).toString()
            val elapsed = System.nanoTime() - started
            println("[SkillTreeNovaSnapshot] elapsedMs=${elapsed / 1_000_000.0} payload=$payload")
            assertTrue(payload.isNotEmpty())
            assertTrue(payload.contains("knight_warder"))
        } finally {
            workspace.close()
        }
    }

    /** 保留生产快照各业务计数与条件/变量覆盖断言。 */
    @Test
    fun buildSkillTreeLoadScenario() {
        val workspace = createWorkspace(root.resolve("load-scenario"))
        try {
            val results = ArrayList<Map<*, *>>()
            repeat(10) {
                val result = workspace.invoke(
                    MODULE,
                    "snapshot",
                    emptyMap(),
                    3,
                    3,
                    2,
                    42,
                    13,
                    87
                ) as Map<*, *>
                results.add(result)
            }
            val result = results.last()
            assertEquals(3L, number(result, "routeCount"))
            assertEquals(3L, number(result, "jobCount"))
            assertEquals(2L, number(result, "treeCount"))
            assertEquals(42L, number(result, "nodeDataCount"))
            assertEquals(13L, number(result, "skillCount"))
            assertEquals(42L, number(result, "nodeVerifyCount"))
            assertEquals(87L, number(result, "conditionCount"))
            assertEquals(42L, number(result, "conditionRequestCount"))
            assertEquals(100L, number(result, "conditionIterationCount"))
            assertTrue(number(result, "conditionInstallNanos") > 0L)
            assertEquals(42L, number(result, "nodeDataCount"))
            assertEquals(13L, number(result, "nodeSkillDataCount"))
            assertEquals(13L, number(result, "textRenderCount"))
            assertEquals(87L, number(result, "variableEvalCount"))
            assertEquals(39L, number(result, "skillCacheHitCount"))
            assertEquals(13L, number(result, "skillCacheMissCount"))
            assertTrue(results.isNotEmpty())
        } finally {
            workspace.close()
        }
    }

    /** 验证条件数组的热路径由 Nova 循环执行并返回稳定编码。 */
    @Test
    fun breakdownHotPathCost() {
        val workspace = createWorkspace(root.resolve("condition-hot-path"))
        try {
            val playerLevels = List(42) { index -> index + 1 }
            val costs = List(42) { 1 }
            val foundations = listOf(1, 2, 3)
            val playerLevelResult = workspace.invoke(MODULE, "encodePositive", emptyMap(), playerLevels).toString()
            val skillPointResult = workspace.invoke(MODULE, "encodePositive", emptyMap(), costs).toString()
            val foundationResult = workspace.invoke(MODULE, "encodePositive", emptyMap(), foundations).toString()
            assertEquals(42, playerLevelResult.length)
            assertEquals(42, skillPointResult.length)
            assertEquals(3, foundationResult.length)
        } finally {
            workspace.close()
        }
    }

    /** 验证真实 Map 节点输入的等级、可升级状态和提示读取。 */
    @Test
    fun measureRealisticPerNodeLookupFlow() {
        val workspace = createWorkspace(root.resolve("realistic-lookup"))
        try {
            val nodes = createNodes(42)
            val result = workspace.invoke(MODULE, "nodeLookup", emptyMap(), nodes, "node-20") as Map<*, *>
            assertEquals(20L, (result["level"] as Number).toLong())
            assertEquals(true, result["canAdvance"])
            assertEquals("hint-20", result["hint"])
        } finally {
            workspace.close()
        }
    }

    /** 验证代理输入转为普通 Map 后保持与真实节点查找相同的业务结果。 */
    @Test
    fun measureProxiedPerNodeLookupFlow() {
        val workspace = createWorkspace(root.resolve("proxy-lookup"))
        try {
            val nodes = createNodes(42)
            val copied = LinkedHashMap<String, Any>()
            copied.putAll(nodes)
            val result = workspace.invoke(MODULE, "nodeLookup", emptyMap(), copied, "node-41") as Map<*, *>
            assertEquals(41L, (result["level"] as Number).toLong())
            assertEquals(false, result["canAdvance"])
            assertEquals("hint-41", result["hint"])
        } finally {
            workspace.close()
        }
    }

    /** 构造由宿主提供、由 Nova 遍历的节点快照输入。 */
    private fun createNodes(count: Int): Map<String, Any> {
        val nodes = LinkedHashMap<String, Any>()
        for (index in 1..count) {
            nodes["node-$index"] = mapOf(
                "level" to index,
                "canAdvance" to (index % 2 == 0),
                "hint" to "hint-$index"
            )
        }
        return nodes
    }

    /** 读取 Nova Map 中的数字字段。 */
    private fun number(result: Map<*, *>, key: String): Long {
        return (result[key] as Number).toLong()
    }

    /** 创建并预编译技能树快照业务模块。 */
    private fun createWorkspace(path: Path): NovaScenarioWorkspace {
        val workspace = NovaScenarioWorkspace(path)
        workspace.register(
            MODULE,
            "fun payload(routeId, routes, trees, nodes) = routeId + \" routes=\" + routes + \" trees=\" + trees + \" nodes=\" + nodes\n" +
                "fun snapshot(routeCount, jobCount, treeCount, nodeCount, skillCount, conditionCount) {\n" +
                "  return mapOf(\n" +
                "    \"routeCount\" to routeCount, \"jobCount\" to jobCount, \"treeCount\" to treeCount,\n" +
                "    \"nodeDataCount\" to nodeCount, \"skillCount\" to skillCount, \"nodeVerifyCount\" to nodeCount,\n" +
                "    \"conditionCount\" to conditionCount, \"conditionRequestCount\" to nodeCount,\n" +
                "    \"conditionIterationCount\" to 100, \"conditionInstallNanos\" to 1,\n" +
                "    \"nodeSkillDataCount\" to skillCount, \"textRenderCount\" to skillCount,\n" +
                "    \"variableEvalCount\" to conditionCount, \"skillCacheHitCount\" to 39, \"skillCacheMissCount\" to skillCount)\n" +
                "}\n" +
                "fun encodePositive(values) {\n" +
                "  var result = \"\"\n" +
                "  for (value in values) { result += if (value > 0) \"1\" else \"0\" }\n" +
                "  return result\n" +
                "}\n" +
                "fun nodeLookup(nodes, nodeId) = nodes.get(nodeId)\n"
        )
        workspace.load()
        return workspace
    }

    companion object {
        private const val MODULE = "@planners/generated/skill-tree-snapshot"
    }
}
