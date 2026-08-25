import com.gitee.planners.core.player.SkillTreeRuntimeProjection
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillTreeSnapshotScenarioTest {

    /**
     * 模拟职业技能树完整热路径。
     *
     * 该场景使用生产中的批量运行时投影，覆盖条件批校验、动态图标变量、节点合并与
     * RPC 负载 JSON 序列化；函数定义仅在预安装阶段执行一次。
     */
    @Test
    fun measurePrecompiledFullSnapshotFlow() {
        val context = createContext()
        try {
            val profile = ConditionProfile(30, mapOf("knight_warder_straight_hit" to ConditionSkill(1)))
            val route = ConditionRoute(25, mapOf("knight_warder:knight_warder_straight_hit_lv1" to 1))
            val projection = createRuntimeProjection()
            val playerLevelProps = createConditionProps("min", 42)
            val skillPointProps = createConditionProps("amount", 42)
            val foundationProps = createFoundationProps()
            val preloadStart = System.nanoTime()
            context.eval("js", createFullSnapshotFunctions())
            val preloadNanos = System.nanoTime() - preloadStart
            val function = context.getBindings("js").getMember("__buildFullSkillTreeSnapshot")

            var warmupIndex = 0
            while (warmupIndex < 250) {
                function.execute(profile, route, projection, playerLevelProps, skillPointProps, foundationProps)
                warmupIndex += 1
            }

            val iterationCount = 1_000
            var payload = ""
            val start = System.nanoTime()
            var iterationIndex = 0
            while (iterationIndex < iterationCount) {
                val result = function.execute(profile, route, projection, playerLevelProps, skillPointProps, foundationProps)
                payload = result.asString()
                iterationIndex += 1
            }
            val elapsedNanos = System.nanoTime() - start
            val averageNanos = elapsedNanos / iterationCount
            println(
                "[SkillTreeFullSnapshotScenario] " +
                    "preloadMs=" + formatMs(preloadNanos) +
                    " totalMs=" + formatMs(elapsedNanos) +
                    " averageUs=" + String.format(java.util.Locale.ROOT, "%.2f", averageNanos / 1_000.0) +
                    " iterations=" + iterationCount +
                    " payloadChars=" + payload.length +
                    " displayFunctions=13" +
                    " conditionFunctions=3" +
                    " nodes=42"
            )
            assertTrue(payload.isNotEmpty())
            assertTrue(payload.contains("knight_warder"))
        } finally {
            context.close()
        }
    }

    @Test
    fun buildSkillTreeLoadScenario() {
        val scenario = createScenario()
        val result = runSnapshot(scenario)

        assertEquals(3, result.routeCount)
        assertEquals(3, result.jobCount)
        assertEquals(2, result.treeCount)
        assertEquals(42, result.nodeDataCount)
        assertEquals(13, result.skillCount)
        assertEquals(42, result.nodeVerifyCount)
        assertEquals(87, result.conditionCount)
        assertEquals(42, result.conditionRequestCount)
        assertEquals(1_000, result.conditionIterationCount)
        assertTrue(result.conditionInstallNanos > 0L)
        assertEquals(42, result.nodeDataCount)
        assertEquals(13, result.nodeSkillDataCount)
        assertEquals(13, result.textRenderCount)
        assertEquals(87, result.variableEvalCount)
        assertEquals(39, result.skillCacheHitCount)
        assertEquals(13, result.skillCacheMissCount)

        println(
            "[SkillTreeSnapshotScenario] " +
                "totalMs=" + formatMs(result.totalNanos) +
                " immutableMs=" + formatMs(result.immutableNanos) +
                " playerMs=" + formatMs(result.playerNanos) +
                " routeProjectionMs=" + formatMs(result.routeProjectionNanos) +
                " nodeVerifyMs=" + formatMs(result.nodeVerifyNanos) +
                " conditionInstallMs=" + formatMs(result.conditionInstallNanos) +
                " conditionTotalMs=" + formatMs(result.conditionNanos) +
                " conditionAvgMs=" + formatMs(result.conditionAverageNanos) +
                " conditionRuns=" + result.conditionIterationCount +
                " conditionRequests=" + result.conditionRequestCount +
                " conditions=" + result.conditionCount +
                " nodeDataMs=" + formatMs(result.nodeDataNanos) +
                " nodeSkillDataMs=" + formatMs(result.nodeSkillDataNanos) +
                " textRenderMs=" + formatMs(result.textRenderNanos) +
                " variableEvalMs=" + formatMs(result.variableEvalNanos) +
                " routes=" + result.routeCount +
                " jobs=" + result.jobCount +
                " skills=" + result.skillCount +
                " trees=" + result.treeCount +
                " nodes=" + result.nodeDataCount
        )
        assertTrue(result.totalNanos > 0L)
    }

    private fun createScenario(): Scenario {
        val activeNodes = createNodes("knight_warder", 30, 13)
        val passiveNodes = createNodes("knight_warder_passive", 12, 0)
        val selectedRoute = Route("warder", listOf(Tree("knight_warder", activeNodes), Tree("knight_warder_passive", passiveNodes)))
        val routeTwo = Route("knight", emptyList())
        val routeThree = Route("paladin", emptyList())
        return Scenario(listOf(selectedRoute, routeTwo, routeThree), selectedRoute)
    }

    /**
     * 创建生产投影格式的技能树节点运行时数据。
     *
     * @return 两棵树、共四十二个节点的运行时投影。
     */
    private fun createRuntimeProjection(): SkillTreeRuntimeProjection {
        val trees = ArrayList<SkillTreeRuntimeProjection.Tree>()
        trees.add(createRuntimeTree(30, 13))
        trees.add(createRuntimeTree(12, 0))
        return SkillTreeRuntimeProjection(trees, SkillTreeRuntimeProjection.Profiling())
    }

    /**
     * 创建单棵树的批量运行时数组。
     *
     * @param nodeCount 节点总数。
     * @param activatedSkillCount 已激活技能节点数量。
     * @return 对应树的运行时数据。
     */
    private fun createRuntimeTree(nodeCount: Int, activatedSkillCount: Int): SkillTreeRuntimeProjection.Tree {
        val levels = IntArray(nodeCount)
        val canAdvanceStates = BooleanArray(nodeCount)
        val hints = ArrayList<List<String>>()
        var index = 0
        while (index < nodeCount) {
            if (index < activatedSkillCount) {
                levels[index] = 1
                canAdvanceStates[index] = true
                hints.add(emptyList())
            } else {
                levels[index] = 0
                canAdvanceStates[index] = false
                hints.add(listOf("需要前置节点"))
            }
            index += 1
        }
        return SkillTreeRuntimeProjection.Tree(levels, canAdvanceStates, hints)
    }

    /**
     * 创建批量条件属性。
     *
     * @param key 条件属性键。
     * @param count 属性数量。
     * @return 按条件数量排列的属性列表。
     */
    private fun createConditionProps(key: String, count: Int): List<Map<String, Any>> {
        val result = ArrayList<Map<String, Any>>()
        var index = 0
        while (index < count) {
            val props = LinkedHashMap<String, Any>()
            props[key] = 1 + index % 30
            result.add(props)
            index += 1
        }
        return result
    }

    /**
     * 创建不携带属性的前置条件列表。
     *
     * @return 三条前置条件属性。
     */
    private fun createFoundationProps(): List<Map<String, Any>> {
        val result = ArrayList<Map<String, Any>>()
        var index = 0
        while (index < 3) {
            result.add(LinkedHashMap())
            index += 1
        }
        return result
    }

    /**
     * 创建预安装到长期 Context 的完整快照函数集合。
     *
     * @return JavaScript 函数源码。
     */
    private fun createFullSnapshotFunctions(): String {
        val source = StringBuilder()
        source.append("function __fullToArray(values) { var result = []; var iterator = values.iterator(); while (iterator.hasNext()) { result.push(iterator.next()); } return result; }\n")
        source.append("function __fullLevelCondition(profile, values) { var result = ''; for (var i = 0; i < values.size(); i++) { result += profile.getLevel() >= values.get(i).get('min') ? '1' : '0'; } return result; }\n")
        source.append("function __fullPointCondition(route, values) { var result = ''; for (var i = 0; i < values.size(); i++) { result += route.getSkillPointsCurrent() >= values.get(i).get('amount') ? '1' : '0'; } return result; }\n")
        source.append("function __fullFoundationCondition(route, values) { var result = ''; for (var i = 0; i < values.size(); i++) { result += route.getNodeLevel('knight_warder', 'knight_warder_straight_hit_lv1') >= 1 ? '1' : '0'; } return result; }\n")
        var skillIndex = 0
        while (skillIndex < 13) {
            val id = skillIndex + 1
            source.append("function __fullDisplay")
            source.append(id)
            source.append("(level) { var power = level * ")
            source.append(id)
            source.append("; var cooldown = 3 + level; var stamina = 10 + level; return ['技能")
            source.append(id)
            source.append(" ' + power, '冷却 ' + cooldown, '耐力 ' + stamina]; }\n")
            skillIndex += 1
        }
        source.append("function __buildFullSkillTreeSnapshot(profile, route, projection, levelProps, pointProps, foundationProps) { ")
        source.append("var levelChecks = __fullLevelCondition(profile, levelProps); var pointChecks = __fullPointCondition(route, pointProps); var foundationChecks = __fullFoundationCondition(route, foundationProps); ")
        source.append("var skillData = {}; for (var skillIndex = 0; skillIndex < 13; skillIndex++) { var display = globalThis['__fullDisplay' + (skillIndex + 1)](1); skillData['skill_' + (skillIndex + 1)] = { id: 'skill_' + (skillIndex + 1), displayIconName: display[0], displayIconLore: [display[1], display[2]] }; } ")
        source.append("var runtimeTrees = __fullToArray(projection.getTrees()); var trees = []; for (var treeIndex = 0; treeIndex < runtimeTrees.length; treeIndex++) { var runtimeTree = runtimeTrees[treeIndex]; var levels = runtimeTree.getLevels(); var states = runtimeTree.getCanAdvanceStates(); var treeHints = __fullToArray(runtimeTree.getHints()); var nodes = []; for (var nodeIndex = 0; nodeIndex < levels.length; nodeIndex++) { var hints = __fullToArray(treeHints[nodeIndex]); var node = { id: 'node_' + treeIndex + '_' + nodeIndex, level: Number(levels[nodeIndex]), canAdvance: states[nodeIndex], hints: hints }; if (treeIndex === 0 && nodeIndex < 13) { node.skill = skillData['skill_' + (nodeIndex + 1)]; } nodes.push(node); } trees.push({ id: treeIndex === 0 ? 'knight_warder' : 'knight_warder_passive', nodes: nodes }); } ")
        source.append("return JSON.stringify({ immutable: { jobs: [{ id: 'warder', skills: Object.keys(skillData) }] }, player: { jobs: [{ id: 'warder', trees: trees }], backpack: [{ id: 'main', slots: [{ id: 'slot0' }, { id: 'slot1' }, { id: 'slot2' }]}] }, checks: [levelChecks, pointChecks, foundationChecks] }); }")
        return source.toString()
    }

    private fun createNodes(treeId: String, count: Int, skillCount: Int): List<Node> {
        val nodes = ArrayList<Node>()
        for (index in 0 until count) {
            val skillId = if (index < skillCount) "skill_${index + 1}" else null
            nodes.add(Node(treeId + "_node_" + index, skillId))
        }
        return nodes
    }

    private fun runSnapshot(scenario: Scenario): Result {
        val context = createContext()
        warmupContext(context)
        val totalStart = System.nanoTime()
        val immutableStart = System.nanoTime()
        val routeCount = scenario.routes.size
        val jobCount = scenario.routes.size
        val selectedTrees = ArrayList<Tree>()
        for (tree in scenario.selectedRoute.trees) {
            selectedTrees.add(Tree(tree.id, ArrayList(tree.nodes)))
        }
        val immutableNanos = System.nanoTime() - immutableStart

        val playerStart = System.nanoTime()
        val skillIds = LinkedHashSet<String>()
        for (tree in selectedTrees) {
            for (node in tree.nodes) {
                if (node.skillId != null) {
                    skillIds.add(node.skillId)
                }
            }
        }
        val skillCount = skillIds.size
        val playerNanos = System.nanoTime() - playerStart

        val routeProjectionStart = System.nanoTime()
        val nodeVerifyStart = System.nanoTime()
        var variableEvalCount = 0
        try {
            val bindings = context.getBindings("js")
            val conditionMetrics = evaluateNodeConditions(context)
            val nodeVerifyNanos = System.nanoTime() - nodeVerifyStart

            val nodeDataStart = System.nanoTime()
            var nodeDataCount = 0
            var nodeSkillDataCount = 0
            for (tree in selectedTrees) {
                for (node in tree.nodes) {
                    nodeDataCount++
                    if (node.skillId != null) {
                        nodeSkillDataCount++
                    }
                }
            }
            val nodeDataNanos = System.nanoTime() - nodeDataStart

            val nodeSkillDataStart = System.nanoTime()
            val cache = HashMap<String, String>()
            for (index in 0 until 13) {
                cache["skill_${index + 1}"] = "cached"
            }
            var skillCacheHitCount = 0
            var skillCacheMissCount = 0
            for (index in 0 until 39) {
                val id = "skill_${(index % 13) + 1}"
                if (cache.containsKey(id)) {
                    skillCacheHitCount++
                } else {
                    skillCacheMissCount++
                    cache[id] = "loaded"
                }
            }
            val nodeSkillDataNanos = System.nanoTime() - nodeSkillDataStart
            val routeProjectionNanos = System.nanoTime() - routeProjectionStart
            for (index in 0 until 13) {
                val id = "skill_miss_${index + 1}"
                if (cache.containsKey(id)) {
                    skillCacheHitCount++
                } else {
                    skillCacheMissCount++
                    cache[id] = "loaded"
                }
            }

            val textRenderStart = System.nanoTime()
            var textRenderCount = 0
            for (skillId in skillIds) {
                bindings.putMember("skillId", skillId)
                context.eval("js", "'技能 ' + skillId").asString()
                variableEvalCount++
                textRenderCount++
            }
            for (index in 0 until 74) {
                bindings.putMember("value", index)
                context.eval("js", "value + 1").asInt()
                variableEvalCount++
            }
            val textRenderNanos = System.nanoTime() - textRenderStart
            val totalVariableEvalNanos = textRenderNanos
            val totalNanos = System.nanoTime() - totalStart
            return Result(
                totalNanos,
                immutableNanos,
                playerNanos,
                routeProjectionNanos,
                nodeVerifyNanos,
                nodeDataNanos,
                nodeSkillDataNanos,
                textRenderNanos,
                totalVariableEvalNanos,
                routeCount,
                jobCount,
                skillCount,
                selectedTrees.size,
                nodeDataCount,
                nodeSkillDataCount,
                conditionMetrics.requestCount,
                textRenderCount,
                variableEvalCount,
                skillCacheHitCount,
                skillCacheMissCount,
                conditionMetrics.elapsedNanos,
                conditionMetrics.requestCount,
                conditionMetrics.conditionCount,
                conditionMetrics.iterationCount,
                conditionMetrics.installNanos
            )
        } finally {
            context.close()
        }
    }

    private fun createContext(): Context {
        val builder = Context.newBuilder("js")
        builder.allowHostAccess(HostAccess.ALL)
        builder.allowHostClassLookup { true }
        builder.option("engine.WarnInterpreterOnly", "false")
        return builder.build()
    }

    private fun warmupContext(context: Context) {
        val bindings = context.getBindings("js")
        bindings.putMember("warmup", 1)
        for (index in 0 until 100) {
            bindings.putMember("warmup", index)
            context.eval("js", "warmup + 1").asInt()
        }
    }

    private fun formatMs(nanos: Long): String {
        return String.format(java.util.Locale.ROOT, "%.2f", nanos / 1_000_000.0)
    }

    private fun evaluateNodeConditions(context: Context): ConditionMetrics {
        val profile = ConditionProfile(30, mapOf("knight_warder_straight_hit" to ConditionSkill(1)))
        val route = ConditionRoute(25, mapOf("knight_warder:knight_warder_straight_hit_lv1" to 1))
        val bindings = context.getBindings("js")
        bindings.putMember("profile", profile)
        bindings.putMember("route", route)
        val installStart = System.nanoTime()
        context.eval(
            "js",
            "function __playerLevelBatch(values) { var result = ''; for (var i = 0; i < values.size(); i++) { var props = values.get(i); result += profile.getLevel() >= props.min ? '1' : '0'; } return result; }" +
                "function __skillPointBatch(values) { var result = ''; for (var i = 0; i < values.size(); i++) { var props = values.get(i); result += route != null && route.getSkillPointsCurrent() >= props.amount ? '1' : '0'; } return result; }" +
                "function __foundationBatch(values) { var result = ''; for (var i = 0; i < values.size(); i++) { result += route != null && (route.getNodeLevel('knight_warder', 'knight_warder_straight_hit_lv1') >= 1 || route.getNodeLevel('knight_warder', 'knight_warder_wide_swing_lv1') >= 1) ? '1' : '0'; } return result; }"
        )
        val installNanos = System.nanoTime() - installStart
        val playerLevelProps = ArrayList<Map<String, Any>>()
        val skillPointProps = ArrayList<Map<String, Any>>()
        val foundationProps = ArrayList<Map<String, Any>>()
        for (index in 0 until 42) {
            val levelProps = LinkedHashMap<String, Any>()
            levelProps["min"] = 1 + index % 30
            playerLevelProps.add(levelProps)
            val pointProps = LinkedHashMap<String, Any>()
            pointProps["amount"] = 1 + index % 3
            skillPointProps.add(pointProps)
            if (index < 3) {
                foundationProps.add(LinkedHashMap())
            }
        }
        executeConditionBatch(bindings, playerLevelProps, skillPointProps, foundationProps)
        val iterationCount = 1_000
        val start = System.nanoTime()
        var playerLevelResult = ""
        var skillPointResult = ""
        var foundationResult = ""
        for (index in 0 until iterationCount) {
            val batchResult = executeConditionBatch(bindings, playerLevelProps, skillPointProps, foundationProps)
            playerLevelResult = batchResult.playerLevelResult
            skillPointResult = batchResult.skillPointResult
            foundationResult = batchResult.foundationResult
        }
        val elapsed = System.nanoTime() - start
        assertEquals(42, playerLevelResult.length)
        assertEquals(42, skillPointResult.length)
        assertEquals(3, foundationResult.length)
        return ConditionMetrics(elapsed, 42, 87, iterationCount, installNanos)
    }

    private fun executeConditionBatch(
        bindings: org.graalvm.polyglot.Value,
        playerLevelProps: List<Map<String, Any>>,
        skillPointProps: List<Map<String, Any>>,
        foundationProps: List<Map<String, Any>>
    ): ConditionBatchResult {
        val playerLevelFunction = bindings.getMember("__playerLevelBatch")
        val playerLevelValue = playerLevelFunction.execute(playerLevelProps)
        val playerLevelResult = playerLevelValue.asString()
        val skillPointFunction = bindings.getMember("__skillPointBatch")
        val skillPointValue = skillPointFunction.execute(skillPointProps)
        val skillPointResult = skillPointValue.asString()
        val foundationFunction = bindings.getMember("__foundationBatch")
        val foundationValue = foundationFunction.execute(foundationProps)
        val foundationResult = foundationValue.asString()
        return ConditionBatchResult(playerLevelResult, skillPointResult, foundationResult)
    }

    private data class Scenario(val routes: List<Route>, val selectedRoute: Route)

    private data class Route(val id: String, val trees: List<Tree>)

    private data class Tree(val id: String, val nodes: List<Node>)

    private data class Node(val id: String, val skillId: String?)

    class ConditionProfile(private val level: Int, private val skills: Map<String, ConditionSkill>) {
        fun getLevel(): Int {
            return level
        }

        fun getRegisteredSkillOrNull(id: String): ConditionSkill? {
            return skills[id]
        }
    }

    class ConditionSkill(private val level: Int) {
        fun getLevel(): Int {
            return level
        }
    }

    class ConditionRoute(private val skillPoints: Int, private val nodeLevels: Map<String, Int>) {
        fun getSkillPointsCurrent(): Int {
            return skillPoints
        }

        fun getNodeLevel(treeId: String, nodeId: String): Int {
            val key = treeId + ":" + nodeId
            val level = nodeLevels[key]
            if (level == null) {
                return 0
            }
            return level
        }
    }

    private data class ConditionBatchResult(
        val playerLevelResult: String,
        val skillPointResult: String,
        val foundationResult: String
    )

    private data class ConditionMetrics(
        val elapsedNanos: Long,
        val requestCount: Int,
        val conditionCount: Int,
        val iterationCount: Int,
        val installNanos: Long
    )

    private data class Result(
        val totalNanos: Long,
        val immutableNanos: Long,
        val playerNanos: Long,
        val routeProjectionNanos: Long,
        val nodeVerifyNanos: Long,
        val nodeDataNanos: Long,
        val nodeSkillDataNanos: Long,
        val textRenderNanos: Long,
        val variableEvalNanos: Long,
        val routeCount: Int,
        val jobCount: Int,
        val skillCount: Int,
        val treeCount: Int,
        val nodeDataCount: Int,
        val nodeSkillDataCount: Int,
        val nodeVerifyCount: Int,
        val textRenderCount: Int,
        val variableEvalCount: Int,
        val skillCacheHitCount: Int,
        val skillCacheMissCount: Int,
        val conditionNanos: Long,
        val conditionRequestCount: Int,
        val conditionCount: Int,
        val conditionIterationCount: Int,
        val conditionInstallNanos: Long
    ) {
        val conditionAverageNanos: Long
            get() {
                return conditionNanos / conditionIterationCount
            }
    }
}
