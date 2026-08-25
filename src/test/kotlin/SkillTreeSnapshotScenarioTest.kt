import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
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

            // 程序内 repeat 30 次：逐次计时，观察预热前后差异
            val repeatCount = 30
            val iterationUs = ArrayList<Double>(repeatCount)
            var payload = ""
            for (iteration in 1..repeatCount) {
                val start = System.nanoTime()
                val result = function.execute(profile, route, projection, playerLevelProps, skillPointProps, foundationProps)
                payload = result.asString()
                iterationUs.add((System.nanoTime() - start) / 1_000.0)
                println(
                    "[SkillTreeFullSnapshotRepeat] iter=" + iteration +
                        " us=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.last())
                )
            }
            val warmupCount = 5
            val warmupAverage = iterationUs.take(warmupCount).average()
            val warmedAverage = iterationUs.drop(warmupCount).average()
            println(
                "[SkillTreeFullSnapshotScenario] " +
                    "preloadMs=" + formatMs(preloadNanos) +
                    " repeats=" + repeatCount +
                    " firstAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", warmupAverage) +
                    " warmedAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", warmedAverage) +
                    " totalAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.average()) +
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
        // 程序内 repeat 30 次：逐次输出流水线总耗时，观察预热前后差异
        val repeats = 30
        val results = ArrayList<Result>(repeats)
        for (iteration in 1..repeats) {
            val runResult = runSnapshot(createScenario())
            results.add(runResult)
            println(
                "[SkillTreeSnapshotRepeat] iter=" + iteration +
                    " totalMs=" + formatMs(runResult.totalNanos) +
                    " conditionTotalMs=" + formatMs(runResult.conditionNanos) +
                    " conditionAvgMs=" + formatMs(runResult.conditionAverageNanos)
            )
        }

        val result = results.last()
        assertEquals(3, result.routeCount)
        assertEquals(3, result.jobCount)
        assertEquals(2, result.treeCount)
        assertEquals(42, result.nodeDataCount)
        assertEquals(13, result.skillCount)
        assertEquals(42, result.nodeVerifyCount)
        assertEquals(87, result.conditionCount)
        assertEquals(42, result.conditionRequestCount)
        assertEquals(100, result.conditionIterationCount)
        assertTrue(result.conditionInstallNanos > 0L)
        assertEquals(42, result.nodeDataCount)
        assertEquals(13, result.nodeSkillDataCount)
        assertEquals(13, result.textRenderCount)
        assertEquals(87, result.variableEvalCount)
        assertEquals(39, result.skillCacheHitCount)
        assertEquals(13, result.skillCacheMissCount)

        val warmupCount = 5
        fun List<Result>.totalMsList(): List<Double> = map { it.totalNanos / 1_000_000.0 }
        val totals = results.totalMsList()
        println(
            "[SkillTreeSnapshotScenario] " +
                "repeats=" + repeats +
                " firstAvgMs=" + String.format(java.util.Locale.ROOT, "%.2f", totals.take(warmupCount).average()) +
                " warmedAvgMs=" + String.format(java.util.Locale.ROOT, "%.2f", totals.drop(warmupCount).average()) +
                " totalAvgMs=" + String.format(java.util.Locale.ROOT, "%.2f", totals.average()) +
                " lastRun: " +
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
        assertTrue(results.first().totalNanos > 0L)
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
    /** 基准场景用的批量运行时投影数据（与原生产投影结构一致）。 */
    class BenchRuntimeProjection(val trees: List<BenchTree>)

    class BenchTree(
        val levels: IntArray,
        val canAdvanceStates: BooleanArray,
        val hints: List<List<String>>
    )

    private fun createRuntimeProjection(): BenchRuntimeProjection {
        val trees = ArrayList<BenchTree>()
        trees.add(createRuntimeTree(30, 13))
        trees.add(createRuntimeTree(12, 0))
        return BenchRuntimeProjection(trees)
    }

    /**
     * 创建单棵树的批量运行时数组。
     *
     * @param nodeCount 节点总数。
     * @param activatedSkillCount 已激活技能节点数量。
     * @return 对应树的运行时数据。
     */
    private fun createRuntimeTree(nodeCount: Int, activatedSkillCount: Int): BenchTree {
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
        return BenchTree(levels, canAdvanceStates, hints)
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
        val iterationCount = 100
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

    /**
     * 热路径分段计时：把完整快照函数拆成独立阶段逐个测量。
     *
     * 先整体预热，再对每个阶段分别采样，定位具体耗时来源。
     */
    @Test
    fun breakdownHotPathCost() {
        val context = createContext()
        try {
            val profile = ConditionProfile(30, mapOf("knight_warder_straight_hit" to ConditionSkill(1)))
            val route = ConditionRoute(25, mapOf("knight_warder:knight_warder_straight_hit_lv1" to 1))
            val projection = createRuntimeProjection()
            val playerLevelProps = createConditionProps("min", 42)
            val skillPointProps = createConditionProps("amount", 42)
            val foundationProps = createFoundationProps()
            context.eval("js", createFullSnapshotFunctions())
            // 分段专用辅助函数
            context.eval(
                "js",
                "function __benchSkillData() { var skillData = {}; for (var i = 0; i < 13; i++) { var display = globalThis['__fullDisplay' + (i + 1)](1); skillData['skill_' + (i + 1)] = { id: 'skill_' + (i + 1), displayIconName: display[0], displayIconLore: [display[1], display[2]] }; } return skillData; }" +
                    "function __benchTrees(projection, skillData) { var runtimeTrees = __fullToArray(projection.getTrees()); var trees = []; for (var treeIndex = 0; treeIndex < runtimeTrees.length; treeIndex++) { var runtimeTree = runtimeTrees[treeIndex]; var levels = runtimeTree.getLevels(); var states = runtimeTree.getCanAdvanceStates(); var treeHints = __fullToArray(runtimeTree.getHints()); var nodes = []; for (var nodeIndex = 0; nodeIndex < levels.length; nodeIndex++) { var hints = __fullToArray(treeHints[nodeIndex]); var node = { id: 'node_' + treeIndex + '_' + nodeIndex, level: Number(levels[nodeIndex]), canAdvance: states[nodeIndex], hints: hints }; if (treeIndex === 0 && nodeIndex < 13) { node.skill = skillData['skill_' + (nodeIndex + 1)]; } nodes.push(node); } trees.push({ id: treeIndex === 0 ? 'knight_warder' : 'knight_warder_passive', nodes: nodes }); } return trees; }" +
                    "function __benchBuild(profile, route, projection, levelProps, pointProps, foundationProps) { var levelChecks = __fullLevelCondition(profile, levelProps); var pointChecks = __fullPointCondition(route, pointProps); var foundationChecks = __fullFoundationCondition(route, foundationProps); var skillData = __benchSkillData(); var trees = __benchTrees(projection, skillData); return JSON.stringify({ immutable: { jobs: [{ id: 'warder', skills: Object.keys(skillData) }] }, player: { jobs: [{ id: 'warder', trees: trees }], backpack: [{ id: 'main', slots: [{ id: 'slot0' }, { id: 'slot1' }, { id: 'slot2' }]}] }, checks: [levelChecks, pointChecks, foundationChecks] }); }" +
                    "function __benchStringify(profile, route, projection, levelProps, pointProps, foundationProps) { var levelChecks = __fullLevelCondition(profile, levelProps); var pointChecks = __fullPointCondition(route, pointProps); var foundationChecks = __fullFoundationCondition(route, foundationProps); var skillData = __benchSkillData(); var trees = __benchTrees(projection, skillData); return JSON.stringify({ immutable: { jobs: [{ id: 'warder', skills: Object.keys(skillData) }] }, player: { jobs: [{ id: 'warder', trees: trees }], backpack: [{ id: 'main', slots: [{ id: 'slot0' }, { id: 'slot1' }, { id: 'slot2' }]}] }, checks: [levelChecks, pointChecks, foundationChecks] }); }"
            )
            val bindings = context.getBindings("js")
            bindings.putMember("profile", profile)
            bindings.putMember("route", route)
            context.eval(
                "js",
                "function __bPlayerLevel(values) { var result = ''; for (var i = 0; i < values.size(); i++) { result += profile.getLevel() >= values.get(i).get('min') ? '1' : '0'; } return result; }" +
                    "function __bSkillPoint(values) { var result = ''; for (var i = 0; i < values.size(); i++) { result += route != null && route.getSkillPointsCurrent() >= values.get(i).get('amount') ? '1' : '0'; } return result; }" +
                    "function __bFoundation() { var result = ''; for (var i = 0; i < 3; i++) { result += route.getNodeLevel('knight_warder', 'knight_warder_straight_hit_lv1') >= 1 ? '1' : '0'; } return result; }"
            )

            // 预构建共享输入，避免把 skillDataBuild 的耗时混进 treeAssembly
            val sharedSkillData = bindings.getMember("__benchSkillData").execute()

            data class Segment(val name: String, val sample: () -> Unit)

            val segments = listOf(
                Segment("level") { bindings.getMember("__fullLevelCondition").execute(profile, playerLevelProps) },
                Segment("point") { bindings.getMember("__fullPointCondition").execute(route, skillPointProps) },
                Segment("foundation") { bindings.getMember("__fullFoundationCondition").execute(route, foundationProps) },
                Segment("skillData") { bindings.getMember("__benchSkillData").execute() },
                Segment("treeAssembly") { bindings.getMember("__benchTrees").execute(projection, sharedSkillData) },
                Segment("fullSnapshot") {
                    bindings.getMember("__buildFullSkillTreeSnapshot").execute(
                        profile, route, projection, playerLevelProps, skillPointProps, foundationProps
                    )
                }
            )

            // 不预热：从冷启动第 1 次起逐轮计时，输出每轮各分段耗时
            val iterations = 30
            for (iteration in 1..iterations) {
                val builder = StringBuilder("[Breakdown] iter=").append(iteration)
                for (segment in segments) {
                    val start = System.nanoTime()
                    segment.sample()
                    val us = (System.nanoTime() - start) / 1_000.0
                    builder.append(' ').append(segment.name).append("Us=")
                        .append(String.format(java.util.Locale.ROOT, "%.2f", us))
                }
                println(builder.toString())
            }
        } finally {
            context.close()
        }
    }

    /**
     * 真实脚本场景基准：不使用批量投影，JS 对宿主对象逐节点反查。
     *
     * 每个节点的等级、可激活状态、提示都各自独立跨语言查询一次，
     * 还原未做批量合并优化时的真实访问模式。
     */
    @Test
    fun measureRealisticPerNodeLookupFlow() {
        val context = createContext()
        try {
            val profile = ConditionProfile(30, mapOf("knight_warder_straight_hit" to ConditionSkill(1)))
            val route = RealLookupRoute(
                skillPoints = 25,
                trees = listOf(
                    RealLookupTree("knight_warder", 30, 13),
                    RealLookupTree("knight_warder_passive", 12, 0)
                )
            )
            val playerLevelProps = createConditionProps("min", 42)
            val skillPointProps = createConditionProps("amount", 42)
            val foundationProps = createFoundationProps()
            context.eval("js", createFullSnapshotFunctions())
            context.eval(
                "js",
                "function __buildRealSkillTreeSnapshot(profile, route, levelProps, pointProps, foundationProps) { " +
                    "var levelChecks = __fullLevelCondition(profile, levelProps); var pointChecks = __fullPointCondition(route, pointProps); var foundationChecks = __fullFoundationCondition(route, foundationProps); " +
                    "var skillData = {}; for (var i = 0; i < 13; i++) { var display = globalThis['__fullDisplay' + (i + 1)](1); skillData['skill_' + (i + 1)] = { id: 'skill_' + (i + 1), displayIconName: display[0], displayIconLore: [display[1], display[2]] }; } " +
                    "var trees = []; for (var treeIndex = 0; treeIndex < route.getTreeCount(); treeIndex++) { var treeId = route.getTreeId(treeIndex); var nodeCount = route.getNodeCount(treeId); var nodes = []; for (var nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) { var nodeId = route.getNodeIdAt(treeId, nodeIndex); var node = { id: 'node_' + treeIndex + '_' + nodeIndex, level: Number(route.getNodeLevelById(treeId, nodeId)), canAdvance: route.isNodeCanAdvance(treeId, nodeId), hints: __fullToArray(route.getNodeHints(treeId, nodeId)) }; if (treeIndex === 0 && nodeIndex < 13) { node.skill = skillData['skill_' + (nodeIndex + 1)]; } nodes.push(node); } trees.push({ id: treeId, nodes: nodes }); } " +
                    "return JSON.stringify({ immutable: { jobs: [{ id: 'warder', skills: Object.keys(skillData) }] }, player: { jobs: [{ id: 'warder', trees: trees }], backpack: [{ id: 'main', slots: [{ id: 'slot0' }, { id: 'slot1' }, { id: 'slot2' }]}] }, checks: [levelChecks, pointChecks, foundationChecks] }); }"
            )
            val function = context.getBindings("js").getMember("__buildRealSkillTreeSnapshot")
            val repeats = 30
            val iterationUs = ArrayList<Double>(repeats)
            var payload = ""
            for (iteration in 1..repeats) {
                val start = System.nanoTime()
                val result = function.execute(profile, route, playerLevelProps, skillPointProps, foundationProps)
                payload = result.asString()
                iterationUs.add((System.nanoTime() - start) / 1_000.0)
                println(
                    "[RealLookupRepeat] iter=" + iteration +
                        " us=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.last())
                )
            }
            check(payload.contains("knight_warder"))
            check(payload.contains("hints"))
            val warmupCount = 5
            println(
                "[SkillTreeRealLookupScenario] " +
                    "repeats=" + repeats +
                    " firstAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.take(warmupCount).average()) +
                    " warmedAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.drop(warmupCount).average()) +
                    " totalAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.average()) +
                    " payloadChars=" + payload.length +
                    " nodes=42"
            )
        } finally {
            context.close()
        }
    }

    /**
     * 真实反查模式的宿主路线对象：所有数据按单节点粒度提供，每次访问都是一次穿越。
     */
    class RealLookupRoute(private val skillPoints: Int, private val trees: List<RealLookupTree>) {

        fun getSkillPointsCurrent(): Int {
            return skillPoints
        }

        fun getNodeLevel(treeId: String, nodeId: String): Int {
            return findTree(treeId).levelOf(nodeId)
        }

        fun getTreeCount(): Int {
            return trees.size
        }

        fun getTreeId(index: Int): String {
            return trees[index].id
        }

        fun getNodeCount(treeId: String): Int {
            return findTree(treeId).levels.size
        }

        fun getNodeIdAt(treeId: String, index: Int): String {
            return findTree(treeId).id + "_node_" + index
        }

        fun getNodeLevelById(treeId: String, nodeId: String): Int {
            return findTree(treeId).levelOf(nodeId)
        }

        fun isNodeCanAdvance(treeId: String, nodeId: String): Boolean {
            return findTree(treeId).canAdvanceOf(nodeId)
        }

        fun getNodeHints(treeId: String, nodeId: String): List<String> {
            return findTree(treeId).hintsOf(nodeId)
        }

        private fun findTree(treeId: String): RealLookupTree {
            for (tree in trees) {
                if (tree.id == treeId) {
                    return tree
                }
            }
            throw IllegalArgumentException("未知技能树: $treeId")
        }
    }

    /** 单棵树的逐节点运行时数据。 */
    class RealLookupTree(val id: String, nodeCount: Int, activatedSkillCount: Int) {

        val levels = IntArray(nodeCount)
        private val canAdvance = BooleanArray(nodeCount)
        private val hints = ArrayList<List<String>>()

        init {
            for (index in 0 until nodeCount) {
                if (index < activatedSkillCount) {
                    levels[index] = 1
                    canAdvance[index] = true
                    hints.add(emptyList())
                } else {
                    levels[index] = 0
                    canAdvance[index] = false
                    hints.add(listOf("需要前置节点"))
                }
            }
        }

        val nodeId: String = id

        fun size(): Int {
            return levels.size
        }

        fun levelOf(nodeId: String): Int {
            val index = nodeId.substringAfterLast('_').toIntOrNull() ?: return 0
            return levels.getOrNull(index) ?: 0
        }

        fun canAdvanceOf(nodeId: String): Boolean {
            val index = nodeId.substringAfterLast('_').toIntOrNull() ?: return false
            return canAdvance.getOrNull(index) ?: false
        }

        fun hintsOf(nodeId: String): List<String> {
            val index = nodeId.substringAfterLast('_').toIntOrNull() ?: return emptyList()
            return hints.getOrNull(index) ?: emptyList()
        }

        private fun indexOf(nodeId: String): Int {
            return nodeId.substringAfterLast('_').toInt()
        }
    }

    /** 只读字符串列表的 polyglot 数组代理：JS 侧按原生数组索引直读。 */
    class StringListProxy(private val data: List<String>) : ProxyArray {

        override fun get(index: Long): Any {
            return data[index.toInt()]
        }

        override fun set(index: Long, value: Value?) {
            throw UnsupportedOperationException("只读")
        }

        override fun remove(index: Long): Boolean {
            throw UnsupportedOperationException("只读")
        }

        override fun getSize(): Long {
            return data.size.toLong()
        }
    }

    /**
     * 代理层反查路线：保持 JS 逐节点反查语义不变，把宿主方法分派从反射改为
     * 预构建的 ProxyExecutable，并把树/节点查找降为 O(1)。
     */
    class ProxyLookupRoute(delegate: RealLookupRoute) : ProxyObject {

        private val members: Map<String, ProxyExecutable>
        private val treeIndexes: Map<String, ProxyTreeView>

        init {
            // 复用 delegate 的数据引用构建 O(1) 索引
            val treeIds = (0 until delegate.getTreeCount()).map { delegate.getTreeId(it) }
            treeIndexes = treeIds.associateWith { id ->
                // 通过 delegate 的公开方法拿不到内部 Tree 引用，这里用同构数据重建索引视图
                val nodeCount = delegate.getNodeCount(id)
                val indexMap = HashMap<String, Int>(nodeCount * 2)
                for (i in 0 until nodeCount) {
                    indexMap[delegate.getNodeIdAt(id, i)] = i
                }
                ProxyTreeView(id, nodeCount, indexMap) { nodeId ->
                    Triple(
                        delegate.getNodeLevelById(id, nodeId),
                        delegate.isNodeCanAdvance(id, nodeId),
                        delegate.getNodeHints(id, nodeId)
                    )
                }
            }
            members = mapOf(
                "getSkillPointsCurrent" to exec { _ -> delegate.getSkillPointsCurrent() },
                "getNodeLevel" to exec { args ->
                    val treeId = args[0].asString()
                    val nodeId = args[1].asString()
                    view(treeId).read(nodeId).first
                },
                "getTreeCount" to exec { _ -> delegate.getTreeCount() },
                "getTreeId" to exec { args -> delegate.getTreeId(args[0].asInt()) },
                "getNodeCount" to exec { args ->
                    view(args[0].asString()).nodeCount
                },
                "getNodeIdAt" to exec { args ->
                    val treeId = args[0].asString()
                    delegate.getNodeIdAt(treeId, args[1].asInt())
                },
                "getNodeLevelById" to exec { args ->
                    view(args[0].asString()).read(args[1].asString()).first
                },
                "isNodeCanAdvance" to exec { args ->
                    view(args[0].asString()).read(args[1].asString()).second
                },
                "getNodeHints" to exec { args ->
                    StringListProxy(view(args[0].asString()).read(args[1].asString()).third)
                }
            )
        }

        private fun view(treeId: String): ProxyTreeView {
            return treeIndexes[treeId] ?: throw IllegalArgumentException("未知技能树: $treeId")
        }

        private fun exec(block: (Array<Value>) -> Any?): ProxyExecutable {
            return ProxyExecutable { arguments -> block(arguments) }
        }

        override fun getMember(key: String?): Any {
            return members[key] ?: throw IllegalArgumentException("未知成员: $key")
        }

        override fun getMemberKeys(): Any {
            return members.keys
        }

        override fun hasMember(key: String?): Boolean {
            return members.containsKey(key)
        }

        override fun putMember(key: String?, value: Value?) {
            throw UnsupportedOperationException("只读")
        }
    }

    /** 单棵树的 O(1) 索引视图。 */
    private class ProxyTreeView(
        val id: String,
        val nodeCount: Int,
        private val indexByNodeId: Map<String, Int>,
        private val readNode: (String) -> Triple<Int, Boolean, List<String>>
    ) {
        fun read(nodeId: String): Triple<Int, Boolean, List<String>> {
            return readNode(nodeId)
        }
    }

    /**
     * 代理层反查基准：与真实反查模式同场景同规模，仅宿主对象换成 ProxyObject 实现，
     * 方法分派走预构建的 ProxyExecutable，树/节点查找 O(1)。
     */
    @Test
    fun measureProxiedPerNodeLookupFlow() {
        val context = createContext()
        try {
            val profile = ConditionProfile(30, mapOf("knight_warder_straight_hit" to ConditionSkill(1)))
            val plainRoute = RealLookupRoute(
                skillPoints = 25,
                trees = listOf(
                    RealLookupTree("knight_warder", 30, 13),
                    RealLookupTree("knight_warder_passive", 12, 0)
                )
            )
            val route: Any = ProxyLookupRoute(plainRoute)
            val playerLevelProps = createConditionProps("min", 42)
            val skillPointProps = createConditionProps("amount", 42)
            val foundationProps = createFoundationProps()
            context.eval("js", createFullSnapshotFunctions())
            context.eval(
                "js",
                "function __proxyToArray(v) { if (v == null) return []; var result = []; for (var i = 0; i < v.length; i++) { result.push(v[i]); } return result; }" +
                    "function __buildProxySkillTreeSnapshot(profile, route, levelProps, pointProps, foundationProps) { " +
                    "var levelChecks = __fullLevelCondition(profile, levelProps); var pointChecks = __fullPointCondition(route, pointProps); var foundationChecks = __fullFoundationCondition(route, foundationProps); " +
                    "var skillData = {}; for (var i = 0; i < 13; i++) { var display = globalThis['__fullDisplay' + (i + 1)](1); skillData['skill_' + (i + 1)] = { id: 'skill_' + (i + 1), displayIconName: display[0], displayIconLore: [display[1], display[2]] }; } " +
                    "var trees = []; for (var treeIndex = 0; treeIndex < route.getTreeCount(); treeIndex++) { var treeId = route.getTreeId(treeIndex); var nodeCount = route.getNodeCount(treeId); var nodes = []; for (var nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) { var nodeId = route.getNodeIdAt(treeId, nodeIndex); var node = { id: 'node_' + treeIndex + '_' + nodeIndex, level: Number(route.getNodeLevelById(treeId, nodeId)), canAdvance: route.isNodeCanAdvance(treeId, nodeId), hints: __proxyToArray(route.getNodeHints(treeId, nodeId)) }; if (treeIndex === 0 && nodeIndex < 13) { node.skill = skillData['skill_' + (nodeIndex + 1)]; } nodes.push(node); } trees.push({ id: treeId, nodes: nodes }); } " +
                    "return JSON.stringify({ immutable: { jobs: [{ id: 'warder', skills: Object.keys(skillData) }] }, player: { jobs: [{ id: 'warder', trees: trees }], backpack: [{ id: 'main', slots: [{ id: 'slot0' }, { id: 'slot1' }, { id: 'slot2' }]}] }, checks: [levelChecks, pointChecks, foundationChecks] }); }"
            )
            val function = context.getBindings("js").getMember("__buildProxySkillTreeSnapshot")
            val repeats = 30
            val iterationUs = ArrayList<Double>(repeats)
            var payload = ""
            for (iteration in 1..repeats) {
                val start = System.nanoTime()
                val result = function.execute(profile, route, playerLevelProps, skillPointProps, foundationProps)
                payload = result.asString()
                iterationUs.add((System.nanoTime() - start) / 1_000.0)
                println(
                    "[ProxyLookupRepeat] iter=" + iteration +
                        " us=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.last())
                )
            }
            check(payload.contains("knight_warder"))
            check(payload.contains("hints"))
            val warmupCount = 5
            println(
                "[SkillTreeProxyLookupScenario] " +
                    "repeats=" + repeats +
                    " firstAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.take(warmupCount).average()) +
                    " warmedAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.drop(warmupCount).average()) +
                    " totalAvgUs=" + String.format(java.util.Locale.ROOT, "%.2f", iterationUs.average()) +
                    " payloadChars=" + payload.length +
                    " nodes=42"
            )
        } finally {
            context.close()
        }
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
