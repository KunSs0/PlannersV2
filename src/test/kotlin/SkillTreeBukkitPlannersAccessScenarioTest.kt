import com.gitee.planners.core.script.proxy.ProxyRouteTarget
import com.gitee.planners.core.script.proxy.ProxyTreeDefinition
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * 模拟 Zeus 技能树快照访问 Bukkit 与 Planners 对象的完整场景。
 *
 * 测试保留生产脚本的 Java Map/List 遍历、对象 getter 调用、批量运行时投影、
 * 技能图标渲染调用与 JSON 序列化顺序，但不依赖 Paper 运行时。
 */
class SkillTreeBukkitPlannersAccessScenarioTest {

    /**
     * 测量经过 Java 宿主对象访问的完整技能树快照热路径。
     */
    @Test
    fun measureBukkitAndPlannersObjectAccessFlow() {
        val scenario = createScenario()
        val context = createContext()
        try {
            installProductionSnapshot(context, scenario)
            val function = context.getBindings("js").getMember("__testSkillTreeSnapshot")
            var warmupIndex = 0
            while (warmupIndex < 250) {
                function.execute(scenario.player, scenario.template, scenario.router)
                warmupIndex += 1
            }

            val iterationCount = 10
            var payload = ""
            val start = System.nanoTime()
            var iterationIndex = 0
            while (iterationIndex < iterationCount) {
                val result = function.execute(scenario.player, scenario.template, scenario.router)
                payload = result.asString()
                iterationIndex += 1
            }
            val elapsedNanos = System.nanoTime() - start
            val averageNanos = elapsedNanos / iterationCount
            println(
                "[SkillTreeBukkitAccessRealLookup] " +
                    "totalMs=" + formatMs(elapsedNanos) +
                    " averageUs=" + String.format(java.util.Locale.ROOT, "%.2f", averageNanos / 1_000.0) +
                    " iterations=" + iterationCount +
                    " payloadChars=" + payload.length +
                    " routes=3" +
                    " skills=13" +
                    " trees=2" +
                    " nodes=42"
            )
            assertTrue(payload.isNotEmpty())
            assertTrue(payload.contains("knight_warder"))
        } finally {
            context.close()
        }
    }

    /**
     * 生产快照管线分段计时：从冷启动第 1 轮起逐轮输出各阶段耗时。
     *
     * 阶段：背包收集 → immutable 数据（含技能图标渲染）→ 玩家职业/树/节点组装 →
     * JSON 序列化；另测纯节点反查环，隔离穿越成本与对象组装成本。
     */
    @Test
    fun breakdownSnapshotStages() {
        val scenario = createScenario()
        val context = createContext()
        try {
            installProductionSnapshot(context, scenario)
            val b = context.getBindings("js")
            context.eval(
                "js",
                "function __stageBackpack(player, template, playerRouter) { return ZeusJs.planners.__bench.collectPlayerBackpackData(template); }" +
                    "function __newCtx() { return { skillDataCache: {}, skillDataById: {} }; }" +
                    "function __stageImmutable(ctx, player, template, playerRouter, backpackData) { return ZeusJs.planners.__bench.toImmutableRouterData(playerRouter.getRouter(), player, template, ctx, null); }" +
                    "function __stagePlayerJobs(ctx, player, template, playerRouter, backpackData) { return ZeusJs.planners.__bench.toSkillTreePlayerData(player, template, playerRouter, ctx, backpackData); }" +
                    "function __stageNodeLoop(player, template, playerRouter) { var view = PlannersJs.route.getPlayerRoute(playerRouter.getCurrentRoute(), player); var sink = 0; for (var t = 0; t < view.getTreeCount(); t++) { var treeId = view.getTreeId(t); var count = view.getNodeCount(treeId); for (var i = 0; i < count; i++) { var nodeId = view.getNodeIdAt(treeId, i); sink += view.getNodeLevel(treeId, nodeId); if (view.canAdvanceNode(treeId, nodeId)) { sink += 1; } sink += view.getNodeHints(treeId, nodeId).length; } } return sink; }"
            )

            // 每轮模拟完整管线顺序：backpack → immutable(填充技能数据缓存) → playerJobs → 纯节点反查环
            val iterations = 30
            for (iteration in 1..iterations) {
                val builder = StringBuilder("[StageBreakdown] iter=").append(iteration)

                fun runStage(fnName: String, args: Array<Any?>): Double {
                    val start = System.nanoTime()
                    b.getMember(fnName).execute(*args)
                    return (System.nanoTime() - start) / 1_000.0
                }

                val backpackUs = runStage("__stageBackpack", arrayOf(scenario.player, scenario.template, scenario.router))
                var backpackData = b.getMember("__stageBackpack").execute(scenario.player, scenario.template, scenario.router)
                // immutable 与 playerJobs 共享同一个 ctx（skillData 缓存由 immutable 填充）
                val ctx = b.getMember("__newCtx").execute()
                val immutableUs = runStage("__stageImmutable", arrayOf(ctx, scenario.player, scenario.template, scenario.router, backpackData))
                val playerJobsUs = runStage("__stagePlayerJobs", arrayOf(ctx, scenario.player, scenario.template, scenario.router, backpackData))
                val nodeLoopUs = runStage("__stageNodeLoop", arrayOf(scenario.player, scenario.template, scenario.router))
                val fullUs = runStage("__testSkillTreeSnapshot", arrayOf(scenario.player, scenario.template, scenario.router))

                builder.append(" backpackUs=").append(String.format(java.util.Locale.ROOT, "%.2f", backpackUs))
                builder.append(" immutableUs=").append(String.format(java.util.Locale.ROOT, "%.2f", immutableUs))
                builder.append(" playerJobsUs=").append(String.format(java.util.Locale.ROOT, "%.2f", playerJobsUs))
                builder.append(" nodeLoopUs=").append(String.format(java.util.Locale.ROOT, "%.2f", nodeLoopUs))
                builder.append(" fullUs=").append(String.format(java.util.Locale.ROOT, "%.2f", fullUs))
                println(builder.toString())
            }
        } finally {
            context.close()
        }
    }

    /**
     * toTreeNodeData 内部分段计时：反查 / 静态字段拼装 / 动态字段拼装 / skill 挂载。
     */
    @Test
    fun breakdownTreeNodeData() {
        val scenario = createScenario()
        val context = createContext()
        try {
            installProductionSnapshot(context, scenario)
            val b = context.getBindings("js")
            context.eval(
                "js",
                "function __nodeBench(player, template, playerRouter, repeats) { " +
                    "var route = playerRouter.getCurrentRoute(); " +
                "var view = PlannersJs.route.getPlayerRoute(route, player); " +
                    "var treeId = view.getTreeId(0); " +
                    "var tree = null; var treeValues = PlannersJs.convert.toArray(route.getSkillTrees()); for (var i = 0; i < treeValues.length; i++) { if (String(treeValues[i].getId()) === treeId) { tree = treeValues[i]; } } " +
                    "var structure = ZeusJs.planners.__bench.getTreeStructure(tree); " +
                    "var ctx = { skillDataCache: {}, skillDataById: {} }; " +
                    "var immutable = ZeusJs.planners.__bench.toImmutableRouterData(playerRouter.getRouter(), player, template, ctx, null); " +
                    // 预取全部输入，只测节点组装本身
                    "var defs = structure.nodes; var levels = []; var advances = []; var hintsArr = []; var nodeIds = []; " +
                    "for (var i = 0; i < defs.length; i++) { var nodeId = view.getNodeIdAt(treeId, i); nodeIds.push(nodeId); levels.push(view.getNodeLevel(treeId, nodeId)); advances.push(view.canAdvanceNode(treeId, nodeId)); hintsArr.push(view.getNodeHints(treeId, nodeId)); } " +
                    "var result = { lookup: [], staticBuild: [], dynamicBuild: [], skillMount: [], full: [] }; " +
                    "for (var r = 0; r < repeats; r++) { " +
                    "  var t0 = performanceNow();" +
                    "  for (var i = 0; i < defs.length; i++) { sink += view.getNodeLevel(treeId, nodeIds[i]); if (view.canAdvanceNode(treeId, nodeIds[i])) { sink += 1; } sink += view.getNodeHints(treeId, nodeIds[i]).length; } " +
                    "  result.lookup.push(performanceNow() - t0); " +
                    "  t0 = performanceNow();" +
                    "  for (var i = 0; i < defs.length; i++) { var d = defs[i]; var obj = { id: d.id, type: d.type, position: { x: d.x, y: d.y }, maxLevel: d.maxLevel, requirements: d.requirements }; } " +
                    "  result.staticBuild.push(performanceNow() - t0); " +
                    "  t0 = performanceNow();" +
                    "  for (var i = 0; i < defs.length; i++) { var obj2 = { level: levels[i], canAdvance: advances[i], hints: hintsArr[i] }; } " +
                    "  result.dynamicBuild.push(performanceNow() - t0); " +
                    "  t0 = performanceNow();" +
                    "  for (var i = 0; i < defs.length; i++) { var sd = ctx.skillDataById[defs[i].skillId]; sink += (sd == null ? 0 : 1); } " +
                    "  result.skillMount.push(performanceNow() - t0); " +
                    "  t0 = performanceNow();" +
                    "  for (var i = 0; i < defs.length; i++) { ZeusJs.planners.__bench.toTreeNodeData(treeId, defs[i], levels[i], advances[i], hintsArr[i], ctx); } " +
                    "  result.full.push(performanceNow() - t0); " +
                    "} " +
                    "return JSON.stringify(result); " +
                    "}"
            )
            // performanceNow 注入：宿主纳秒时钟，避免 JS Date 精度问题
            b.putMember("performanceNow", ProxyExecutable { arguments -> System.nanoTime() / 1_000.0 })
            var sink = 0
            b.putMember("sink", sink)

            val iterations = 30
            for (iteration in 1..iterations) {
                val start = System.nanoTime()
                val result = b.getMember("__nodeBench")
                    .execute(scenario.player, scenario.template, scenario.router, 10)
                val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
                println("[NodeDataBreakdown] iter=$iteration totalMs=" + String.format(java.util.Locale.ROOT, "%.2f", elapsedMs) + " " + result.asString())
            }
        } finally {
            context.close()
        }
    }

    /**
     * 创建与当前职业技能树数据规模一致的宿主对象图。
     *
     * @return 可供 GraalJS 访问的完整测试场景。
     */
    private fun createScenario(): Scenario {
        val skills = createSkills()
        val activeTree = createTree("knight_warder", 30, 13, skills)
        val passiveTree = createTree("knight_warder_passive", 12, 0, skills)
        val routeDefinitions = LinkedHashMap<String, MockRouteDefinition>()
        val warderJob = MockJob("warder", "卫士", skills)
        val knightJob = MockJob("knight", "骑士", emptyList())
        val paladinJob = MockJob("paladin", "圣骑士", emptyList())
        val warderDefinition = MockRouteDefinition("warder", warderJob, listOf("knight_warder", "knight_warder_passive"))
        val knightDefinition = MockRouteDefinition("knight", knightJob, emptyList())
        val paladinDefinition = MockRouteDefinition("paladin", paladinJob, emptyList())
        warderDefinition.setBranches(listOf(knightDefinition, paladinDefinition))
        routeDefinitions[warderDefinition.getId()] = warderDefinition
        routeDefinitions[knightDefinition.getId()] = knightDefinition
        routeDefinitions[paladinDefinition.getId()] = paladinDefinition
        val immutableRouter = MockImmutableRouter("knight", "骑士", routeDefinitions)
        val registeredSkills = LinkedHashMap<String, MockPlayerSkill>()
        for (skill in skills) {
            registeredSkills[skill.getId()] = MockPlayerSkill(skill.getId(), 0)
        }
        val playerRoute = MockPlayerRoute(
            "warder",
            registeredSkills,
            listOf(activeTree, passiveTree)
        )
        val playerRouter = MockPlayerRouter(immutableRouter, listOf(playerRoute))
        val template = MockTemplate(playerRouter, registeredSkills)
        val registry = MockRegistry(skills, createBackpack())
        return Scenario(
            MockPlayer("Dev"),
            template,
            playerRouter,
            MockSkillRenderer(),
            MockSkillTreeNodeEffectService(),
            MockBukkit(),
            registry,
            MockBackpack(),
            MockPerformance()
        )
    }

    /**
     * 创建十三个职业技能定义。
     *
     * @return 按职业技能列表顺序组织的技能定义。
     */
    private fun createSkills(): List<MockSkill> {
        val result = ArrayList<MockSkill>()
        var index = 0
        while (index < 13) {
            val id = index + 1
            result.add(MockSkill("skill_$id", "技能$id", 0, 10, listOf("active")))
            index += 1
        }
        return result
    }

    /**
     * 创建一棵技能树静态定义。
     *
     * @param id 技能树 ID。
     * @param nodeCount 节点数量。
     * @param skillNodeCount 技能节点数量。
     * @param skills 可用技能定义。
     * @return 完整的技能树定义。
     */
    private fun createTree(id: String, nodeCount: Int, skillNodeCount: Int, skills: List<MockSkill>): MockTree {
        val nodes = LinkedHashMap<String, MockNode>()
        var index = 0
        while (index < nodeCount) {
            val nodeId = id + "_node_" + index
            val position = MockPosition(index % 6, index / 6)
            if (index < skillNodeCount) {
                nodes[nodeId] = MockNode(nodeId, "skill", position, skills[index])
            } else {
                nodes[nodeId] = MockNode(nodeId, "attribute", position, null)
            }
            index += 1
        }
        return MockTree(id, id, "base", nodes)
    }

    /**
     * 创建与当前职业技能栏相同结构的背包配置。
     *
     * @return 背包页面与槽位定义。
     */
    private fun createBackpack(): MockBackpackDefinition {
        val pages = LinkedHashMap<String, MockBackpackPage>()
        val slots = LinkedHashMap<String, MockBackpackSlot>()
        var index = 0
        while (index < 8) {
            val id = "slot$index"
            slots[id] = MockBackpackSlot(id, "key_$index", listOf("active"))
            index += 1
        }
        pages["0"] = MockBackpackPage("0", "主要技能栏", slots)
        pages["1"] = MockBackpackPage("1", "备用技能栏", slots)
        return MockBackpackDefinition(pages)
    }

    /**
     * 创建用于宿主对象访问模拟的 GraalJS Context。
     *
     * @return 开启 Java 宿主访问的 GraalJS Context。
     */
    private fun createContext(): Context {
        val builder = Context.newBuilder("js")
        builder.allowHostAccess(HostAccess.ALL)
        builder.allowHostClassLookup { true }
        builder.option("engine.WarnInterpreterOnly", "false")
        return builder.build()
    }

    /**
     * 加载生产 Zeus 快照脚本，并将仅依赖 Paper 运行时的三个静态入口替换为测试宿主对象。
     *
     * 快照的对象遍历、缓存、投影、数据组装与 JSON 序列化均直接执行生产脚本，
     * 不在测试中维护第二份实现。
     *
     * @param context GraalJS Context。
     * @param scenario 测试场景。
     */
    private fun installProductionSnapshot(context: Context, scenario: Scenario) {
        val resource = javaClass.getResourceAsStream("/zeus-script/zeus.planners.js")
        if (resource == null) {
            error("缺少测试资源: /zeus-script/zeus.planners.js")
        }
        val reader = resource.bufferedReader(Charsets.UTF_8)
        val source: String
        try {
            source = reader.readText()
        } finally {
            reader.close()
        }
        val rewritten = source
            .replace(
                "var DynamicSkillIcon = Java.type(\"com.gitee.planners.core.skill.formatter.DynamicSkillIcon\").Companion;",
                "var DynamicSkillIcon = MockDynamicSkillIcon;"
            )
            .replace(
                "var SkillTreeNodeEffectService = Java.type(\"com.gitee.planners.core.skilltree.SkillTreeNodeEffectService\").INSTANCE;",
                "var SkillTreeNodeEffectService = MockSkillTreeNodeEffectService;"
            )
            .replace(
                "var Bukkit = Java.type(\"org.bukkit.Bukkit\");",
                "var Bukkit = MockBukkit;"
            )
        assertTrue(!rewritten.contains("Java.type(\"com.gitee.planners.core.skill.formatter.DynamicSkillIcon\")"))
        assertTrue(!rewritten.contains("Java.type(\"com.gitee.planners.core.skilltree.SkillTreeNodeEffectService\")"))
        // 在 IIFE 闭包内部暴露管线函数供分段计时
        val injectIndex = rewritten.lastIndexOf("})();")
        val benchExport = "globalThis.__zeusBench = {" +
            "collectPlayerBackpackData: collectPlayerBackpackData," +
            "toImmutableRouterData: toImmutableRouterData," +
            "toSkillTreePlayerData: toSkillTreePlayerData," +
            "toPlayerJobData: toPlayerJobData," +
            "getTreeStructure: getTreeStructure," +
            "toTreeNodeData: toTreeNodeData" +
            "};"
        val rewrittenWithBench = rewritten.substring(0, injectIndex) + benchExport + rewritten.substring(injectIndex)

        val bindings = context.getBindings("js")
        bindings.putMember("__toScriptObject", ProxyExecutable { arguments -> arguments[0] })
        bindings.putMember("MockDynamicSkillIcon", scenario.renderer)
        bindings.putMember("MockSkillTreeNodeEffectService", scenario.skillLevels)
        bindings.putMember("MockBukkit", scenario.bukkit)
        bindings.putMember("MockRegistry", scenario.registry)
        bindings.putMember("MockBackpack", scenario.backpack)
        bindings.putMember("MockPerformance", scenario.performance)
        context.eval(
            "js",
            """
                var ZeusJs = {
                    performance: {
                        nowNanos: function () { return MockPerformance.nowNanos(); },
                        elapsedMs: function (started) { return MockPerformance.elapsedMs(started); }
                    }
                };
                var PlannersJs = {
                    convert: {
                        toArray: function (values) { return MockRegistry.toArray(values); },
                        mapToObject: function (values) { return MockRegistry.mapToObject(values); }
                    },
                    registry: {
                        get: function (type, id) { return MockRegistry.get(type, id); },
                        backpack: function () { return MockRegistry.getBackpack(); },
                        cached: function (namespace, key, builder) { return MockRegistry.cached(namespace, key, builder); }
                    },
                    route: {
                        getPlayerRoute: function (route, player) { return route.getPlayerRoute(player); }
                    },
                    backpack: {
                        currentPage: function (template) { return MockBackpack.currentPage(template); }
                    }
                };
            """
        )
        context.eval("js", rewrittenWithBench)
        context.eval(
            "js",
            "ZeusJs.planners.__bench = globalThis.__zeusBench;" +
                "function __testSkillTreeSnapshot(player, template, playerRouter) { return JSON.stringify(ZeusJs.planners.skillTreeSnapshot(player, template, playerRouter)); }"
        )
    }

    /**
     * 将纳秒格式化为毫秒文本。
     *
     * @param nanos 纳秒值。
     * @return 两位小数的毫秒文本。
     */
    private fun formatMs(nanos: Long): String {
        return String.format(java.util.Locale.ROOT, "%.2f", nanos / 1_000_000.0)
    }


    /**
     * 技能树访问场景入口对象。
     *
     * @property player 模拟 Bukkit 玩家。
     * @property template 模拟 Planners 玩家档案。
     * @property router 模拟 Planners 玩家职业路由。
     * @property renderer 模拟动态技能图标渲染器。
     */
    private class Scenario(
        val player: MockPlayer,
        val template: MockTemplate,
        val router: MockPlayerRouter,
        val renderer: MockSkillRenderer,
        val skillLevels: MockSkillTreeNodeEffectService,
        val bukkit: MockBukkit,
        val registry: MockRegistry,
        val backpack: MockBackpack,
        val performance: MockPerformance
    )

    /**
     * 模拟 Bukkit 玩家。
     *
     * @property name 玩家名称。
     */
    class MockPlayer(private val name: String) {

        /**
         * 返回玩家名称。
         *
         * @return 玩家名称。
         */
        fun getName(): String {
            return name
        }

        fun getUniqueId(): String {
            return "00000000-0000-0000-0000-000000000001"
        }
    }

    /**
     * 模拟不可变职业路由定义。
     *
     * @property id 职业系 ID。
     * @property name 职业系名称。
     * @property routes 职业阶段定义。
     */
    class MockImmutableRouter(private val id: String, private val name: String, private val routes: Map<String, MockRouteDefinition>) {

        fun getId(): String {
            return id
        }

        fun getName(): String {
            return name
        }

        fun getOriginate(): MockRouteDefinition? {
            return routes["warder"]
        }

        /**
         * 返回职业阶段定义。
         *
         * @return 按配置顺序排列的职业阶段定义。
         */
        fun getRoutes(): Map<String, MockRouteDefinition> {
            return routes
        }
    }

    /**
     * 模拟不可变职业阶段定义。
     *
     * @property id 职业阶段 ID。
     * @property job 职业定义。
     * @property skillTreeIds 技能树 ID 列表。
     */
    class MockRouteDefinition(private val id: String, private val job: MockJob, private val skillTreeIds: List<String>) {

        private var branches: List<MockRouteDefinition> = emptyList()

        fun setBranches(values: List<MockRouteDefinition>) {
            branches = values
        }

        /**
         * 返回职业阶段 ID。
         *
         * @return 职业阶段 ID。
         */
        fun getId(): String {
            return id
        }

        /**
         * 返回职业定义。
         *
         * @return 职业定义。
         */
        fun getJob(): MockJob {
            return job
        }

        /**
         * 返回分支职业 ID。
         *
         * @return 分支职业 ID 列表。
         */
        fun getBranches(): List<MockRouteDefinition> {
            return branches
        }

        /**
         * 返回技能树 ID。
         *
         * @return 技能树 ID 列表。
         */
        fun getSkillTreeIds(): List<String> {
            return skillTreeIds
        }

        fun getIconItemId(): String {
            return "minecraft:iron_sword"
        }

        fun getIcon(): MockItemStack {
            return MockItemStack("minecraft:iron_sword", id + " 图标", listOf("职业线路图标"))
        }
    }

    /**
     * 模拟职业定义。
     *
     * @property id 职业 ID。
     * @property name 职业名称。
     * @property skills 职业技能定义。
     */
    class MockJob(private val id: String, private val name: String, private val skills: List<MockSkill>) {

        /**
         * 返回职业名称。
         *
         * @return 职业名称。
         */
        fun getName(): String {
            return name
        }

        /**
         * 返回职业技能定义。
         *
         * @return 职业技能定义。
         */
        fun getImmutableSkillValues(): List<MockSkill> {
            return skills
        }

        fun getDisplayIconName(): String {
            return name
        }

        fun getDisplayIconLore(): List<String> {
            return listOf("职业说明")
        }

        fun getIcon(): MockItemStack {
            return MockItemStack("minecraft:shield", name, listOf("职业说明"))
        }
    }

    /**
     * 模拟不可变技能定义。
     *
     * @property id 技能 ID。
     * @property name 技能名称。
     * @property startedLevel 初始等级。
     * @property maxLevel 最大等级。
     * @property categories 技能分类。
     */
    class MockSkill(private val id: String, private val name: String, private val startedLevel: Int, private val maxLevel: Int, private val categories: List<String>) {

        /**
         * 返回技能 ID。
         *
         * @return 技能 ID。
         */
        fun getId(): String {
            return id
        }

        /**
         * 返回技能名称。
         *
         * @return 技能名称。
         */
        fun getName(): String {
            return name
        }

        /**
         * 返回技能初始等级。
         *
         * @return 技能初始等级。
         */
        fun getStartedLevel(): Int {
            return startedLevel
        }

        /**
         * 返回技能最大等级。
         *
         * @return 技能最大等级。
         */
        fun getMaxLevel(): Int {
            return maxLevel
        }

        /**
         * 返回技能分类。
         *
         * @return 技能分类。
         */
        fun getCategories(): List<String> {
            return categories
        }

        fun getIconItemId(): String {
            return "minecraft:iron_sword"
        }

        fun getIcon(): MockItemStack {
            return MockItemStack("minecraft:diamond_sword", name, listOf("技能图标"))
        }
    }

    /**
     * 模拟技能树节点。
     *
     * @property id 节点 ID。
     * @property type 节点类型。
     * @property position 节点坐标。
     * @property skill 技能节点对应技能；属性节点为 null。
     */
    class MockNode(private val id: String, private val type: String, private val position: MockPosition, private val skill: MockSkill?) {

        /**
         * 返回节点 ID。
         *
         * @return 节点 ID。
         */
        fun getId(): String {
            return id
        }

        /**
         * 返回节点类型。
         *
         * @return 节点类型。
         */
        fun getType(): String {
            return type
        }

        /**
         * 返回节点坐标。
         *
         * @return 节点坐标。
         */
        fun getPosition(): MockPosition {
            return position
        }

        /**
         * 返回技能节点引用的技能。
         *
         * @return 技能定义；属性节点返回 null。
         */
        fun getSkillId(): String? {
            if (skill == null) {
                return null
            }
            return skill.getId()
        }

        /**
         * 返回节点最大等级。
         *
         * @return 节点最大等级。
         */
        fun getMaxLevel(): Int {
            return 10
        }

        fun getProviderId(): String {
            return "attribute_provider"
        }

        fun getProviderValues(): Map<String, String> {
            val values = LinkedHashMap<String, String>()
            values["attribute"] = "DEF"
            values["amount"] = "5"
            return values
        }
    }

    /**
     * 模拟技能树节点坐标。
     *
     * @property x 横向坐标。
     * @property y 纵向坐标。
     */
    class MockPosition(private val x: Int, private val y: Int) {

        /**
         * 返回横向坐标。
         *
         * @return 横向坐标。
         */
        fun getX(): Int {
            return x
        }

        /**
         * 返回纵向坐标。
         *
         * @return 纵向坐标。
         */
        fun getY(): Int {
            return y
        }
    }

    /**
     * 模拟不可变技能树定义。
     *
     * @property id 技能树 ID。
     * @property name 技能树名称。
     * @property type 技能树类型。
     * @property nodes 节点定义。
     */
    class MockTree(private val id: String, private val name: String, private val type: String, private val nodes: Map<String, MockNode>) {

        /**
         * 返回技能树 ID。
         *
         * @return 技能树 ID。
         */
        fun getId(): String {
            return id
        }

        /**
         * 返回技能树名称。
         *
         * @return 技能树名称。
         */
        fun getName(): String {
            return name
        }

        /**
         * 返回技能树类型。
         *
         * @return 技能树类型。
         */
        fun getType(): String {
            return type
        }

        /**
         * 返回节点定义。
         *
         * @return 按配置顺序排列的节点定义。
         */
        fun getNodes(): Map<String, MockNode> {
            return nodes
        }

        fun getGraph(): Map<String, List<MockRequirement>> {
            val graph = LinkedHashMap<String, List<MockRequirement>>()
            var previous: String? = null
            for (nodeId in nodes.keys) {
                if (previous != null) {
                    graph[nodeId] = listOf(MockRequirement(previous, 1))
                }
                previous = nodeId
            }
            return graph
        }
    }

    /**
     * 模拟玩家技能记录。
     *
     * @property id 技能 ID。
     * @property level 玩家记录等级。
     */
    class MockPlayerSkill(val id: String, val level: Int)

    /**
     * 模拟玩家职业阶段。
     *
     * 节点运行时状态按业务粒度逐节点存储，通过动态路线对象
     * 暴露给脚本逐个反查。
     */
    class MockPlayerRoute(
        private val jobId: String,
        private val registeredSkills: Map<String, MockPlayerSkill>,
        private val trees: List<MockTree>
    ) : ProxyRouteTarget<MockPlayer> {

        private val levelsByNodeId = LinkedHashMap<String, LinkedHashMap<String, Int>>()
        private val canAdvanceByNodeId = LinkedHashMap<String, LinkedHashMap<String, Boolean>>()
        private val hintsByNodeId = LinkedHashMap<String, LinkedHashMap<String, List<String>>>()
        private val treeDefinitions = trees.map { MockTreeDefinition(it) }

        init {
            for (tree in trees) {
                val nodeIds = tree.getNodes().keys.toList()
                levelsByNodeId[tree.getId()] = LinkedHashMap()
                canAdvanceByNodeId[tree.getId()] = LinkedHashMap()
                hintsByNodeId[tree.getId()] = LinkedHashMap()
                for ((index, nodeId) in nodeIds.withIndex()) {
                    val activated = index < 13 && tree.getId() == "knight_warder"
                    levelsByNodeId[tree.getId()]!![nodeId] = if (activated) 1 else 0
                    canAdvanceByNodeId[tree.getId()]!![nodeId] = activated
                    hintsByNodeId[tree.getId()]!![nodeId] = if (activated) emptyList() else listOf("需要前置节点")
                }
            }
        }

        override val proxySkillTrees: List<ProxyTreeDefinition>
            get() = treeDefinitions

        override fun getNodeLevel(treeId: String, nodeId: String): Int {
            return levelsByNodeId[treeId]?.get(nodeId) ?: 0
        }

        override fun isNodeCanAdvance(player: MockPlayer, treeId: String, nodeId: String): Boolean {
            return canAdvanceByNodeId[treeId]?.get(nodeId) ?: false
        }

        override fun getNodeHints(player: MockPlayer, treeId: String, nodeId: String): List<String> {
            return hintsByNodeId[treeId]?.get(nodeId) ?: emptyList()
        }

        /**
         * 返回职业阶段 ID。
         *
         * @return 职业阶段 ID。
         */
        fun getJobId(): String {
            return jobId
        }

        fun getBindingId(): Long {
            return 1L
        }

        fun getParentId(): Long {
            return -1L
        }

        /**
         * 返回已注册技能。
         *
         * @return 已注册技能。
         */
        fun getRegisteredSkill(): Map<String, MockPlayerSkill> {
            return registeredSkills
        }

        /**
         * 返回技能树定义。
         *
         * @return 技能树定义。
         */
        fun getSkillTrees(): List<MockTree> {
            return trees
        }

        /**
         * 返回逐节点反查的动态业务视图。
         *
         * @param player 当前玩家。
         * @return 生产代理层视图。
         */
        fun getPlayerRoute(player: MockPlayer): Any {
            return MockPlayerRouteView(this, player)
        }
    }

    /** 原生 Polyglot 测试上下文中的动态路线对象。 */
    private class MockPlayerRouteView(
        private val route: MockPlayerRoute,
        private val player: MockPlayer
    ) : ProxyObject {

        private val members = setOf(
            "getTreeCount",
            "getTreeId",
            "getNodeCount",
            "getNodeIdAt",
            "getNodeLevel",
            "canAdvanceNode",
            "getNodeHints"
        )

        override fun getMember(key: String?): Any {
            if (key == "getTreeCount") {
                return ProxyExecutable { route.proxySkillTrees.size }
            }
            if (key == "getTreeId") {
                return ProxyExecutable { args -> route.proxySkillTrees[args[0].asInt()].id }
            }
            if (key == "getNodeCount") {
                return ProxyExecutable { args -> tree(args[0].asString()).nodeIds.size }
            }
            if (key == "getNodeIdAt") {
                return ProxyExecutable { args ->
                    val definition = tree(args[0].asString())
                    definition.nodeIds[args[1].asInt()]
                }
            }
            if (key == "getNodeLevel") {
                return ProxyExecutable { args -> route.getNodeLevel(args[0].asString(), args[1].asString()) }
            }
            if (key == "canAdvanceNode") {
                return ProxyExecutable { args -> route.isNodeCanAdvance(player, args[0].asString(), args[1].asString()) }
            }
            if (key == "getNodeHints") {
                return ProxyExecutable { args ->
                    val hints = route.getNodeHints(player, args[0].asString(), args[1].asString())
                    hints.toTypedArray()
                }
            }
            throw IllegalArgumentException("未知成员: $key")
        }

        private fun tree(treeId: String): ProxyTreeDefinition {
            for (definition in route.proxySkillTrees) {
                if (definition.id == treeId) {
                    return definition
                }
            }
            throw IllegalArgumentException("未知技能树: $treeId")
        }

        override fun getMemberKeys(): Any {
            return members
        }

        override fun hasMember(key: String?): Boolean {
            return members.contains(key)
        }

        override fun putMember(key: String?, value: org.graalvm.polyglot.Value?) {
            throw UnsupportedOperationException("只读")
        }
    }

    /** Mock 技能树定义的最小视图。 */
    private class MockTreeDefinition(tree: MockTree) : ProxyTreeDefinition {

        override val id: String = tree.getId()

        override val nodeIds: List<String> = tree.getNodes().keys.toList()
    }

    /**
     * 模拟玩家职业系。
     *
     * @property router 不可变职业系定义。
     * @property routeLine 当前职业线路。
     */
    class MockPlayerRouter(private val router: MockImmutableRouter, private val routeLine: List<MockPlayerRoute>) {

        /**
         * 返回不可变职业系定义。
         *
         * @return 不可变职业系定义。
         */
        fun getRouter(): MockImmutableRouter {
            return router
        }

        /**
         * 返回当前职业线路。
         *
         * @return 当前职业线路。
         */
        fun getRouteLine(): List<MockPlayerRoute> {
            return routeLine
        }

        /**
         * 返回当前职业阶段。
         *
         * @return 当前职业阶段。
         */
        fun getCurrentRoute(): MockPlayerRoute {
            return routeLine[0]
        }

        fun getBindingId(): Long {
            return 1L
        }

        fun getRouterId(): String {
            return router.getId()
        }

        fun getLevel(): Int {
            return 30
        }

        fun getExperience(): Long {
            return 50_000L
        }

        fun getExperienceMax(player: MockPlayer): Long {
            return 60_000L
        }

        fun getMinLevel(): Int {
            return 1
        }

        fun getMaxLevel(): Int {
            return 60
        }

        fun getSkillPointsCurrent(): Int {
            return 10
        }

        fun getSkillPointsUsed(): Int {
            return 20
        }

        fun getCurrentRouteId(): Long {
            return 1L
        }
    }

    /**
     * 模拟 Planners 玩家档案。
     *
     * @property router 玩家职业系。
     * @property registeredSkills 已注册技能。
     */
    class MockTemplate(val router: MockPlayerRouter, val registeredSkills: Map<String, MockPlayerSkill>) {

        /**
         * 返回指定技能的当前有效等级。
         *
         * @param skillId 技能 ID。
         * @return 当前有效等级。
         */
        fun getSkillLevel(skillId: String): Int {
            val skill = registeredSkills[skillId]
            if (skill == null) {
                return 0
            }
            return skill.level + 1
        }

        /**
         * 返回背包槽位投影。
         *
         * @return 三个技能槽位。
         */
        fun getEquippedSkillsForPage(pageId: String): Map<String, MockPlayerSkill> {
            val result = LinkedHashMap<String, MockPlayerSkill>()
            var index = 0
            for (skill in registeredSkills.values) {
                if (index >= 8) {
                    break
                }
                result["slot$index"] = skill
                index += 1
            }
            return result
        }
    }

    /**
     * 模拟动态技能图标渲染器。
     */
    class MockSkillRenderer {

        /**
         * 渲染技能图标文本。
         *
         * @param player 当前玩家。
         * @param skill 技能定义。
         * @param level 当前技能等级。
         * @return 已渲染的图标文本。
         */
        fun render(player: MockPlayer, skill: MockSkill, level: Int): MockRenderedIcon {
            val name = skill.getName() + " Lv" + level
            val lore = ArrayList<String>()
            lore.add("玩家 " + player.getName())
            lore.add("等级 " + level)
            return MockRenderedIcon(name, lore, MockIconProfiling())
        }
    }

    /**
     * 模拟已渲染的技能图标。
     *
     * @property name 图标名称。
     * @property lore 图标描述。
     */
    class MockRenderedIcon(
        private val name: String,
        private val lore: List<String>,
        private val profiling: MockIconProfiling
    ) {

        /**
         * 返回图标名称。
         *
         * @return 图标名称。
         */
        fun getName(): String {
            return name
        }

        /**
         * 返回图标描述。
         *
         * @return 图标描述。
         */
        fun getLore(): List<String> {
            return lore
        }

        fun getProfiling(): MockIconProfiling {
            return profiling
        }
    }

    /** 模拟技能树前置节点要求。 */
    class MockRequirement(private val nodeId: String, private val minLevel: Int) {

        fun getNodeId(): String {
            return nodeId
        }

        fun getMinLevel(): Int {
            return minLevel
        }
    }

    /** 模拟 Bukkit ItemStack。 */
    class MockItemStack(private val materialKey: String, private val name: String?, private val lore: List<String>?) {

        fun getType(): MockMaterial {
            return MockMaterial(materialKey)
        }

        fun getItemMeta(): MockItemMeta {
            return MockItemMeta(name, lore)
        }
    }

    /** 模拟 Bukkit Material。 */
    class MockMaterial(private val key: String) {

        fun getKey(): MockNamespacedKey {
            return MockNamespacedKey(key)
        }
    }

    /** 模拟 Bukkit NamespacedKey。 */
    class MockNamespacedKey(private val value: String) {

        override fun toString(): String {
            return value
        }
    }

    /** 模拟 Bukkit ItemMeta。 */
    class MockItemMeta(private val name: String?, private val lore: List<String>?) {

        fun hasDisplayName(): Boolean {
            return name != null
        }

        fun getDisplayName(): String? {
            return name
        }

        fun hasLore(): Boolean {
            return lore != null && lore.isNotEmpty()
        }

        fun getLore(): List<String>? {
            return lore
        }
    }

    /** 模拟动态技能图标的性能数据对象。 */
    class MockIconProfiling {

        fun getOptionsCreateMs(): Double {
            return 0.0
        }

        fun getScriptContextMs(): Double {
            return 0.0
        }

        fun getScriptSessionOpenMs(): Double {
            return 0.0
        }

        fun getScriptSessionCloseMs(): Double {
            return 0.0
        }

        fun getScriptSessionRebindMs(): Double {
            return 0.0
        }

        fun getVariableEvalMs(): Double {
            return 0.0
        }

        fun getVariableEvalCount(): Int {
            return 0
        }

        fun getTemplateResolveMs(): Double {
            return 0.0
        }

        fun getTemplateResolveCount(): Int {
            return 0
        }

        fun getColorizeMs(): Double {
            return 0.0
        }

        fun getColorizeCount(): Int {
            return 0
        }
    }

    /** 模拟 SkillTreeNodeEffectService 的技能等级读取。 */
    class MockSkillTreeNodeEffectService {

        fun getSkillLevel(template: MockTemplate, skillId: String): Int {
            return template.getSkillLevel(skillId)
        }
    }

    /** 模拟 Bukkit 静态 tick 查询。 */
    class MockBukkit {

        fun getCurrentTick(): Int {
            return 1
        }
    }

    /** 模拟 Zeus 性能 API。 */
    class MockPerformance {

        fun nowNanos(): Long {
            return System.nanoTime()
        }

        fun elapsedMs(startedAt: Long): Double {
            return (System.nanoTime() - startedAt) / 1_000_000.0
        }
    }

    /** 模拟 Planners 背包 API。 */
    class MockBackpack {

        fun currentPage(template: MockTemplate): String {
            return "0"
        }
    }

    /** 模拟 Planners registry 与 convert API。 */
    class MockRegistry(skills: List<MockSkill>, private val backpack: MockBackpackDefinition) {

        private val skillById = LinkedHashMap<String, MockSkill>()

        /** 配置级缓存：namespace → key → value，脚本上下文内永续（reload 即重建）。 */
        private val cacheNamespaces = HashMap<String, HashMap<String, Any?>>()

        init {
            for (skill in skills) {
                skillById[skill.getId()] = skill
            }
        }

        fun cached(namespace: String, key: String, builder: org.graalvm.polyglot.Value): Any? {
            val store = cacheNamespaces.getOrPut(namespace) { HashMap() }
            if (store.containsKey(key)) {
                return store[key]
            }
            val built = builder.execute()
            store[key] = built
            return built
        }

        fun get(type: String, id: String): Any? {
            if (type == "skill") {
                return skillById[id]
            }
            if (type == "keybinding") {
                return MockKeyBinding(id, id.uppercase())
            }
            return null
        }

        fun getBackpack(): MockBackpackDefinition {
            return backpack
        }

        fun toArray(values: Any?): List<Any?> {
            if (values == null) {
                return emptyList()
            }
            if (values is Collection<*>) {
                return ArrayList(values)
            }
            if (values is ProxyArray) {
                val result = ArrayList<Any?>()
                for (index in 0 until values.getSize()) {
                    @Suppress("UNCHECKED_CAST")
                    result.add(values.get(index.toLong()) as Any?)
                }
                return result
            }
            if (values is IntArray) {
                val result = ArrayList<Any?>()
                for (value in values) {
                    result.add(value)
                }
                return result
            }
            if (values is BooleanArray) {
                val result = ArrayList<Any?>()
                for (value in values) {
                    result.add(value)
                }
                return result
            }
            throw IllegalArgumentException("不支持转换为数组的类型: ${values.javaClass.name}")
        }

        fun mapToObject(values: Map<String, String>): Map<String, String> {
            return LinkedHashMap(values)
        }
    }

    /** 模拟 Planners 背包定义。 */
    class MockBackpackDefinition(private val pages: Map<String, MockBackpackPage>) {

        fun getPages(): Map<String, MockBackpackPage> {
            return pages
        }
    }

    /** 模拟 Planners 背包页面。 */
    class MockBackpackPage(
        private val id: String,
        private val name: String,
        private val slots: Map<String, MockBackpackSlot>
    ) {

        fun getId(): String {
            return id
        }

        fun getName(): String {
            return name
        }

        fun getSlots(): Map<String, MockBackpackSlot> {
            return slots
        }
    }

    /** 模拟 Planners 背包槽位。 */
    class MockBackpackSlot(private val id: String, private val key: String, private val categories: List<String>) {

        fun getId(): String {
            return id
        }

        fun getKey(): String {
            return key
        }

        fun getCategories(): List<String> {
            return categories
        }
    }

    /** 模拟 Planners 键位定义。 */
    class MockKeyBinding(private val id: String, private val name: String) {

        fun getName(): String {
            return name
        }
    }
}
