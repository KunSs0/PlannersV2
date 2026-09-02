import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * 使用生产规模的合成数据复现技能树快照中的技能文本渲染与节点查询负载。
 *
 * 该测试保持生产调用拓扑：每个技能调用一次变量脚本，每个模板表达式单独调用一次脚本，
 * 但不读取 server-main 业务配置，也不构造 Bukkit ItemStack。
 */
class DynamicSkillIconNovaWorkloadScenarioTest {

    @TempDir
    lateinit var root: Path

    /** 验证生产规模的预编译 Nova 调用次数、返回数据与耗时报告。 */
    @Test
    fun executeProductionScaleRenderAndNodeLookup() {
        val workspace = NovaScenarioWorkspace(root.resolve("dynamic-skill-icon-workload"))
        workspace.register(MODULE, createNovaSource())

        val compileStarted = System.nanoTime()
        workspace.load()
        val compileNanos = System.nanoTime() - compileStarted

        try {
            val firstResult = executeScenario(workspace)
            val result = executeScenario(workspace)

            assertEquals(ROUTE_COUNT, result.routeCount)
            assertEquals(SKILL_COUNT, result.skillCount)
            assertEquals(VARIABLE_COUNT, result.variableCount)
            assertEquals(TEMPLATE_COUNT, result.templateCount)
            assertEquals(TREE_COUNT, result.treeCount)
            assertEquals(NODE_COUNT, result.nodeCount)
            assertEquals(VARIABLE_SCRIPT_CALL_COUNT, result.variableScriptCallCount)
            assertEquals(TEMPLATE_SCRIPT_CALL_COUNT, result.templateScriptCallCount)
            assertEquals(NODE_SCRIPT_CALL_COUNT, result.nodeScriptCallCount)
            assertTrue(result.renderChecksum > 0L)
            assertTrue(result.nodeChecksum > 0L)

            val compileMs = compileNanos / NANOS_PER_MILLISECOND
            val firstTotalMs =
                (firstResult.renderNanos + firstResult.nodeLookupNanos) / NANOS_PER_MILLISECOND
            val renderMs = result.renderNanos / NANOS_PER_MILLISECOND
            val nodeLookupMs = result.nodeLookupNanos / NANOS_PER_MILLISECOND
            val totalMs = (result.renderNanos + result.nodeLookupNanos) / NANOS_PER_MILLISECOND
            println(
                "[DynamicSkillIconNovaWorkload] " +
                    "compileMs=$compileMs firstTotalMs=$firstTotalMs " +
                    "routes=${result.routeCount} skills=${result.skillCount} " +
                    "variables=${result.variableCount} templates=${result.templateCount} " +
                    "trees=${result.treeCount} nodes=${result.nodeCount} " +
                    "renderCalls=${result.variableScriptCallCount + result.templateScriptCallCount} " +
                    "nodeCalls=${result.nodeScriptCallCount} renderMs=$renderMs " +
                    "nodeLookupMs=$nodeLookupMs totalMs=$totalMs"
            )
        } finally {
            workspace.close()
        }
    }

    /** 执行一轮与生产规模一致的变量、模板和节点脚本调用。 */
    private fun executeScenario(workspace: NovaScenarioWorkspace): ScenarioResult {
        var variableCount = 0
        var templateCount = 0
        var variableScriptCallCount = 0
        var templateScriptCallCount = 0
        var renderChecksum = 0L

        val renderStarted = System.nanoTime()
        for (skillIndex in VARIABLE_COUNTS.indices) {
            val variableResult = workspace.invokePure(
                MODULE,
                "skillVariables$skillIndex",
                SKILL_LEVEL
            )
            assertTrue(variableResult is List<*>)
            val variables = variableResult as List<*>
            val expectedVariableCount = VARIABLE_COUNTS[skillIndex]
            assertEquals(expectedVariableCount, variables.size)
            variableCount += variables.size
            variableScriptCallCount += 1

            val skillTemplateCount = TEMPLATE_COUNTS[skillIndex]
            for (templateIndex in 0 until skillTemplateCount) {
                val variableIndex = templateIndex % variables.size
                val variable = variables[variableIndex]
                val rendered = workspace.invokePure(
                    MODULE,
                    "skillTemplate${skillIndex}_$templateIndex",
                    SKILL_LEVEL,
                    variable
                )
                val renderedText = rendered.toString()
                assertTrue(renderedText.startsWith("Lv$SKILL_LEVEL:"))
                renderChecksum += renderedText.length
                templateCount += 1
                templateScriptCallCount += 1
            }
        }
        val renderNanos = System.nanoTime() - renderStarted

        var nodeCount = 0
        var nodeScriptCallCount = 0
        var nodeChecksum = 0L
        val nodeLookupStarted = System.nanoTime()
        for (treeIndex in TREE_NODE_COUNTS.indices) {
            val treeNodeCount = TREE_NODE_COUNTS[treeIndex]
            for (nodeIndex in 0 until treeNodeCount) {
                val nodeResult = workspace.invokePure(
                    MODULE,
                    "nodeInfo",
                    treeIndex,
                    nodeIndex,
                    SKILL_LEVEL
                )
                assertTrue(nodeResult is Map<*, *>)
                val node = nodeResult as Map<*, *>
                assertEquals("tree-$treeIndex", node["treeId"])
                assertEquals("node-$nodeIndex", node["nodeId"])
                nodeChecksum += (node["level"] as Number).toLong()
                nodeCount += 1
                nodeScriptCallCount += 1
            }
        }
        val nodeLookupNanos = System.nanoTime() - nodeLookupStarted

        return ScenarioResult(
            ROUTE_COUNT,
            VARIABLE_COUNTS.size,
            variableCount,
            templateCount,
            TREE_NODE_COUNTS.size,
            nodeCount,
            variableScriptCallCount,
            templateScriptCallCount,
            nodeScriptCallCount,
            renderChecksum,
            nodeChecksum,
            renderNanos,
            nodeLookupNanos
        )
    }

    /** 生成 13 个变量函数、57 个模板函数和一个节点查询函数。 */
    private fun createNovaSource(): String {
        val source = StringBuilder()
        for (skillIndex in VARIABLE_COUNTS.indices) {
            source.append("fun skillVariables")
            source.append(skillIndex)
            source.append("(level) {\n")
            source.append("  return [")

            val skillVariableCount = VARIABLE_COUNTS[skillIndex]
            for (variableIndex in 0 until skillVariableCount) {
                if (variableIndex > 0) {
                    source.append(", ")
                }
                val factor = variableIndex + 1
                val offset = skillIndex + variableIndex
                source.append("(level * ")
                source.append(factor)
                source.append(" + ")
                source.append(offset)
                source.append(')')
            }
            source.append("]\n")
            source.append("}\n")

            val skillTemplateCount = TEMPLATE_COUNTS[skillIndex]
            for (templateIndex in 0 until skillTemplateCount) {
                source.append("fun skillTemplate")
                source.append(skillIndex)
                source.append('_')
                source.append(templateIndex)
                source.append("(level, value) = \"Lv\" + level + \":\" + value\n")
            }
        }
        source.append("fun nodeInfo(treeIndex, nodeIndex, level) {\n")
        source.append("  return mapOf(\n")
        source.append("    \"treeId\" to \"tree-\" + treeIndex,\n")
        source.append("    \"nodeId\" to \"node-\" + nodeIndex,\n")
        source.append("    \"level\" to level,\n")
        source.append("    \"canAdvance\" to (nodeIndex % 2 == 0)\n")
        source.append("  )\n")
        source.append("}\n")
        return source.toString()
    }

    /** 一轮合成业务调用的计数和耗时。 */
    private class ScenarioResult(
        val routeCount: Int,
        val skillCount: Int,
        val variableCount: Int,
        val templateCount: Int,
        val treeCount: Int,
        val nodeCount: Int,
        val variableScriptCallCount: Int,
        val templateScriptCallCount: Int,
        val nodeScriptCallCount: Int,
        val renderChecksum: Long,
        val nodeChecksum: Long,
        val renderNanos: Long,
        val nodeLookupNanos: Long
    )

    companion object {
        private const val MODULE = "@planners/generated/dynamic-skill-icon-workload"
        private const val ROUTE_COUNT = 3
        private const val SKILL_COUNT = 13
        private const val VARIABLE_COUNT = 87
        private const val TEMPLATE_COUNT = 57
        private const val TREE_COUNT = 2
        private const val NODE_COUNT = 42
        private const val VARIABLE_SCRIPT_CALL_COUNT = 13
        private const val TEMPLATE_SCRIPT_CALL_COUNT = 57
        private const val NODE_SCRIPT_CALL_COUNT = 42
        private const val SKILL_LEVEL = 3
        private const val NANOS_PER_MILLISECOND = 1_000_000.0

        private val VARIABLE_COUNTS = intArrayOf(8, 9, 10, 3, 9, 6, 11, 7, 7, 3, 6, 2, 6)
        private val TEMPLATE_COUNTS = intArrayOf(6, 5, 7, 2, 5, 6, 6, 5, 3, 2, 4, 2, 4)
        private val TREE_NODE_COUNTS = intArrayOf(21, 21)
    }
}
