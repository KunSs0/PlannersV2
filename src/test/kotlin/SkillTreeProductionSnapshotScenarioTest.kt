import com.gitee.planners.Planners
import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.Registries
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableKeyBinding
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.scriptengine.api.ContextPreset
import com.gitee.scriptengine.api.HostAccessMode
import com.gitee.scriptengine.dependency.GraalJsDependencyInstaller
import com.gitee.scriptengine.api.ScriptSession
import com.gitee.scriptengine.api.WorkspaceConfig
import com.gitee.scriptengine.runtime.ScriptWorkspaces
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.service.PlatformIO
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.sql.Connection
import java.sql.DriverManager
import java.util.function.Predicate

/**
 * 使用生产 Planners 配置与对象模型模拟技能树快照。
 *
 * Bukkit 服务端和玩家由 MockBukkit 提供，职业、技能、条件、技能树、玩家路线、
 * 动态图标和 ScriptEngine 均使用生产实现。
 */
class SkillTreeProductionSnapshotScenarioTest {

    /**
     * 测量真实 Planners 对象经过生产 Zeus 快照脚本的完整热路径。
     */
    @Test
    fun measureProductionPlannersSnapshotFlow() {
        try {
            val scenario = createScenario()
            try {
                val productionSequence = measureColdSequence(scenario)
                val sharedEngineSamples = measureSnapshot(scenario)
                val zeusWorkspaceSamples = measureZeusWorkspaceSnapshot(scenario)
                printMockPerformanceReport(sharedEngineSamples, scenario, "SHARED_ENGINE")
                printMockPerformanceReport(zeusWorkspaceSamples, scenario, "Zeus DEFAULT 长会话")
                printColdMockPerformanceReport(productionSequence, scenario, "SHARED_ENGINE")
                assertTrue(sharedEngineSamples.isNotEmpty())
                assertTrue(zeusWorkspaceSamples.isNotEmpty())
                assertTrue(productionSequence.size == 30)
            } finally {
                PlayerTemplateAPI.remove(scenario.player.uniqueId)
                clearRegistries()
                ScriptManager.shutdown()
                MockBukkit.unmock()
            }
        } catch (error: Throwable) {
            error.printStackTrace()
            if (MockBukkit.isMocked()) {
                MockBukkit.unmock()
            }
            throw error
        } finally {
            if (MockBukkit.isMocked()) {
                MockBukkit.unmock()
            }
        }
    }

    /**
     * 执行一组技能树快照热态采样。
     *
     * @param scenario 当前生产对象场景。
     * @return 按采样顺序返回的快照统计结果。
     */
    private fun measureSnapshot(scenario: Scenario): List<SnapshotSample> {
        val session = ScriptManager.openSession(ScriptOptions.of())
        try {
            installProductionSnapshot(session)
            return measureInstalledSnapshot(session, scenario)
        } finally {
            session.close()
        }
    }

    /**
     * 使用 Zeus 服务端相同的 DEFAULT 独立 Context 与非共享 Engine 测量快照。
     *
     * @param scenario 当前生产对象场景。
     * @return 按采样顺序返回的快照统计结果。
     */
    private fun measureZeusWorkspaceSnapshot(scenario: Scenario): List<SnapshotSample> {
        val config = WorkspaceConfig(
            ContextPreset.DEFAULT,
            HostAccessMode.ALL,
            Predicate { true },
            false,
            emptyList(),
            emptyList(),
            emptyMap(),
            SkillTreeProductionSnapshotScenarioTest::class.java.classLoader
        )
        val scriptRoot = createBundledScriptRoot()
        val workspace = ScriptWorkspaces.create(
            scriptRoot.toFile(),
            config
        )
        val session = workspace.createSession()
        try {
            installProductionSnapshot(session)
            return measureInstalledSnapshot(session, scenario)
        } finally {
            session.close()
            workspace.close()
            deleteRecursively(scriptRoot)
        }
    }

    /**
     * 在已装载生产快照函数的长会话中完成预热和热态采样。
     *
     * @param session 已加载快照函数的会话。
     * @param scenario 当前生产对象场景。
     * @return 一百次热态快照样本。
     */
    private fun measureInstalledSnapshot(session: ScriptSession, scenario: Scenario): List<SnapshotSample> {
        var warmupIndex = 0
        while (warmupIndex < 100) {
            ScriptManager.invoke(
                session,
                "__productionSkillTreeSnapshotProfile",
                scenario.player,
                scenario.template,
                scenario.router
            )
            warmupIndex += 1
        }
        val samples = ArrayList<SnapshotSample>()
        var index = 0
        while (index < 100) {
            val invokeStart = System.nanoTime()
            val value = ScriptManager.invoke(
                session,
                "__productionSkillTreeSnapshotProfile",
                scenario.player,
                scenario.template,
                scenario.router
            )
            val invokeMs = elapsedMs(invokeStart)
            samples.add(parseSnapshotSample(value.toString(), invokeMs))
            index += 1
        }
        printRuntimeProfiling(session, scenario)
        return samples
    }

    /**
     * 输出一次热态快照携带的 Java 纳秒级技能树统计。
     *
     * 该调用不计入基准样本，只验证统计对象能跨生产 JS 快照正确传递。
     */
    private fun printRuntimeProfiling(session: ScriptSession, scenario: Scenario) {
        val value = ScriptManager.invoke(
            session,
            "__productionSkillTreeSnapshotProfile",
            scenario.player,
            scenario.template,
            scenario.router
        )
        val root = JsonParser.parseString(value.toString()).asJsonObject
        val profiling = root.getAsJsonObject("profiling")
        if (profiling == null) {
            println("[SkillTreeProductionSnapshotScenario][技能树微秒拆分] 已由生产快照移除")
            return
        }
        val details = profiling.getAsJsonArray("skillTreeRuntimeDetails")
        println("[SkillTreeProductionSnapshotScenario][技能树微秒拆分] " + details)
    }

    /**
     * 模拟生产日志中的连续打开行为，不预热并逐次记录三十个请求。
     *
     * @param scenario 当前生产对象场景。
     * @return 从冷启动开始的连续快照样本。
     */
    private fun measureColdSequence(scenario: Scenario): List<SnapshotSample> {
        val session = ScriptManager.openSession(ScriptOptions.of())
        try {
            installProductionSnapshot(session)
            val samples = ArrayList<SnapshotSample>()
            var index = 0
            while (index < 30) {
                val invokeStart = System.nanoTime()
                val value = ScriptManager.invoke(
                    session,
                    "__productionSkillTreeSnapshotProfile",
                    scenario.player,
                    scenario.template,
                    scenario.router
                )
                val invokeMs = elapsedMs(invokeStart)
                val sample = parseSnapshotSample(value.toString(), invokeMs)
                samples.add(sample)
                println(
                    "[SkillTreeProductionSnapshotScenario][连续请求] " +
                        "index=" + (index + 1) +
                        " invokeMs=" + formatMilliseconds(sample.invokeMs) +
                        " snapshotMs=" + formatMilliseconds(sample.snapshotMs) +
                        " immutableMs=" + formatMilliseconds(sample.immutableMs) +
                        " playerMs=" + formatMilliseconds(sample.playerMs) +
                        " routeProjectionMs=" + formatMilliseconds(sample.routeProjectionMs)
                )
                index += 1
            }
            return samples
        } finally {
            session.close()
        }
    }

    /**
     * 输出零预热、连续三十次生产对象快照的分位数报告。
     *
     * 会话已创建且生产脚本已加载，但不执行任何业务预热调用；因此首轮包含 JIT
     * 与对象访问路径的自然升温。稳定区间固定丢弃前三次，单独反映后续打开表现。
     *
     * @param samples 从零预热开始的连续三十次样本。
     * @param scenario 当前生产场景规模。
     * @param mode 运行时场景名称。
     */
    private fun printColdMockPerformanceReport(
        samples: List<SnapshotSample>,
        scenario: Scenario,
        mode: String
    ) {
        printColdPerformanceSection(samples, scenario, mode, "全部30次", 0)

        val stableSamples = ArrayList<SnapshotSample>()
        for (index in 3 until samples.size) {
            stableSamples.add(samples[index])
        }
        printColdPerformanceSection(stableSamples, scenario, mode, "稳定区间", 3)
    }

    /**
     * 输出一段零预热样本的端到端与阶段分位数。
     *
     * @param samples 当前区间样本。
     * @param scenario 当前生产场景规模。
     * @param mode 运行时场景名称。
     * @param range 区间名称。
     * @param discardedLeadingSamples 已丢弃的前导样本数。
     */
    private fun printColdPerformanceSection(
        samples: List<SnapshotSample>,
        scenario: Scenario,
        mode: String,
        range: String,
        discardedLeadingSamples: Int
    ) {
        val average = SnapshotSample.average(samples)
        val invokeValues = ArrayList<Double>()
        val snapshotValues = ArrayList<Double>()
        val serializationValues = ArrayList<Double>()
        for (sample in samples) {
            invokeValues.add(sample.invokeMs)
            snapshotValues.add(sample.snapshotMs)
            serializationValues.add(sample.serializationMs)
        }
        val boundaryAverage = nonNegative(
            average.invokeMs - average.snapshotMs - average.serializationMs
        )
        println(
            "[SkillTreeMockColdReport] " +
                "mode=" + mode +
                " warmup=0" +
                " range=" + range +
                " samples=" + samples.size +
                " discardLeading=" + discardedLeadingSamples +
                " payloadChars=" + average.payloadChars +
                " routes=" + scenario.routeCount +
                " skills=" + scenario.skillCount +
                " trees=" + scenario.treeCount +
                " nodes=" + scenario.nodeCount
        )
        println(
            "[SkillTreeMockColdReport][端到端ms] " +
                "avg=" + formatMilliseconds(average.invokeMs) +
                " p50=" + formatMilliseconds(percentile(invokeValues, 0.50)) +
                " p95=" + formatMilliseconds(percentile(invokeValues, 0.95)) +
                " max=" + formatMilliseconds(maximum(invokeValues))
        )
        println(
            "[SkillTreeMockColdReport][阶段均值ms] " +
                "snapshot=" + formatMilliseconds(average.snapshotMs) +
                " snapshotP95=" + formatMilliseconds(percentile(snapshotValues, 0.95)) +
                " serialization=" + formatMilliseconds(average.serializationMs) +
                " serializationP95=" + formatMilliseconds(percentile(serializationValues, 0.95)) +
                " javaJsBoundary=" + formatMilliseconds(boundaryAverage)
        )
    }

    /**
     * 输出 MockBukkit 生产对象快照的端到端分位数报告。
     *
     * 每个样本已完成预热，端到端包含 Java 调用、生产 Zeus 快照脚本和结果转换；
     * JSON 序列化单独列出，便于与 RPC 载荷成本区分。
     */
    private fun printMockPerformanceReport(
        samples: List<SnapshotSample>,
        scenario: Scenario,
        mode: String
    ) {
        val average = SnapshotSample.average(samples)
        val invokeValues = ArrayList<Double>()
        val snapshotValues = ArrayList<Double>()
        val serializationValues = ArrayList<Double>()
        for (sample in samples) {
            invokeValues.add(sample.invokeMs)
            snapshotValues.add(sample.snapshotMs)
            serializationValues.add(sample.serializationMs)
        }
        val boundaryAverage = nonNegative(
            average.invokeMs - average.snapshotMs - average.serializationMs
        )
        println(
            "[SkillTreeMockReport] " +
                "mode=" + mode +
                " samples=" + samples.size +
                " warmup=100" +
                " payloadChars=" + average.payloadChars +
                " routes=" + scenario.routeCount +
                " skills=" + scenario.skillCount +
                " trees=" + scenario.treeCount +
                " nodes=" + scenario.nodeCount
        )
        println(
            "[SkillTreeMockReport][端到端ms] " +
                "avg=" + formatMilliseconds(average.invokeMs) +
                " p50=" + formatMilliseconds(percentile(invokeValues, 0.50)) +
                " p95=" + formatMilliseconds(percentile(invokeValues, 0.95)) +
                " max=" + formatMilliseconds(maximum(invokeValues))
        )
        println(
            "[SkillTreeMockReport][阶段均值ms] " +
                "snapshot=" + formatMilliseconds(average.snapshotMs) +
                " snapshotP95=" + formatMilliseconds(percentile(snapshotValues, 0.95)) +
                " serialization=" + formatMilliseconds(average.serializationMs) +
                " serializationP95=" + formatMilliseconds(percentile(serializationValues, 0.95)) +
                " javaJsBoundary=" + formatMilliseconds(boundaryAverage)
        )
    }

    /** 返回样本指定分位点，使用向上取整保证 P95 不低估尾部。 */
    private fun percentile(values: List<Double>, fraction: Double): Double {
        if (values.isEmpty()) {
            error("性能样本为空")
        }
        val ordered = ArrayList(values)
        ordered.sort()
        val index = kotlin.math.ceil((ordered.size - 1) * fraction).toInt()
        return ordered[index]
    }

    /** 返回样本的最大值。 */
    private fun maximum(values: List<Double>): Double {
        if (values.isEmpty()) {
            error("性能样本为空")
        }
        var result = values[0]
        for (index in 1 until values.size) {
            val value = values[index]
            if (value > result) {
                result = value
            }
        }
        return result
    }

    /**
     * 基于当前服务端 Planners 配置创建真实职业技能树运行时对象。
     *
     * @return 生产对象测试场景。
     */
    private fun createScenario(): Scenario {
        val plannersDirectory = File(requiredTestPath("planners.test.plannersRoot"))
        assertTrue(plannersDirectory.isDirectory, "未找到服务端 Planners 配置目录: $plannersDirectory")
        val server = MockBukkit.mock()
        installPlatformIo(plannersDirectory)
        clearRegistries()
        ScriptManager.shutdown()
        GraalJsDependencyInstaller.install(Path.of(requiredTestPath("planners.test.graalJsRoot")))
        ScriptManager.init()

        val config = Configuration.loadFromFile(File(plannersDirectory, "config.yml"))
        configurePlannersNodes(config)
        val routerConfig = Configuration.loadFromFile(File(plannersDirectory, "router/knight.yml"))
        val routeJobIds = readRouteJobIds(routerConfig)
        loadJobs(plannersDirectory, routeJobIds)
        val skillIds = collectRouteSkillIds(routeJobIds)
        loadSkills(plannersDirectory, skillIds)
        val skillTreeIds = readRouteSkillTreeIds(routerConfig, routeJobIds)
        loadSkillTrees(plannersDirectory, skillTreeIds)
        loadKeyBindings(config)
        loadRouter(routerConfig)

        val player = createPlayer(server)
        val router = loadProductionPlayerRouter(plannersDirectory)
        val template = PlayerTemplate(router.userId, player, router, emptyMap())
        PlayerTemplateAPI[player.uniqueId] = template
        val route = router.currentRoute
        val treeCount = route.skillTrees.size
        var nodeCount = 0
        for (tree in route.skillTrees) {
            nodeCount += tree.nodes.size
        }
        return Scenario(player, template, router, routeJobIds.size, 13, treeCount, nodeCount)
    }

    /**
     * 配置测试需要的 Planners ConfigNode 值。
     *
     * @param config 当前服务端使用的 config.yml。
     */
    private fun configurePlannersNodes(config: Configuration) {
        val categories = config.getConfigurationSection("settings.skill.categories")
        if (categories == null) {
            error("Planners 配置缺少 settings.skill.categories")
        }
        Planners.skillCategorySpecs.reset(categories)
        val conditions = config.getConfigurationSection("settings.condition")
        if (conditions == null) {
            error("Planners 配置缺少 settings.condition")
        }
        Planners.conditions.reset(conditions)
        val backpack = config.getConfigurationSection("settings.keybinding.backpack")
        if (backpack == null) {
            error("Planners 配置缺少 settings.keybinding.backpack")
        }
        Planners.backpackConfig.reset(backpack)
    }

    /**
     * 解码服务端的全部技能定义，保证技能树和职业引用均为真实对象。
     *
     * @param plannersDirectory 服务端 Planners 配置根目录。
     */
    private fun loadSkills(plannersDirectory: File, skillIds: Set<String>) {
        val files = File(plannersDirectory, "skill").walkTopDown().filter { it.isFile && it.extension == "yml" }.toList()
        for (file in files) {
            if (!skillIds.contains(file.nameWithoutExtension)) {
                continue
            }
            val skill = ImmutableSkill(Configuration.loadFromFile(file))
            Registries.SKILL[skill.id] = skill
        }
    }

    /**
     * 解码服务端的全部技能树定义。
     *
     * @param plannersDirectory 服务端 Planners 配置根目录。
     */
    private fun loadSkillTrees(plannersDirectory: File, skillTreeIds: Set<String>) {
        val files = File(plannersDirectory, "skilltree").walkTopDown().filter { it.isFile && it.extension == "yml" }.toList()
        for (file in files) {
            val config = Configuration.loadFromFile(file)
            for (key in config.getKeys(false)) {
                if (!skillTreeIds.contains(key)) {
                    continue
                }
                val section = config.getConfigurationSection(key)
                if (section == null) {
                    error("技能树配置不是节点: ${file.absolutePath}/$key")
                }
                val tree = ImmutableSkillTree.parse(key, section)
                Registries.SKILL_TREE[tree.id] = tree
            }
        }
    }

    /**
     * 解码服务端的全部职业定义。
     *
     * @param plannersDirectory 服务端 Planners 配置根目录。
     */
    private fun loadJobs(plannersDirectory: File, jobIds: Set<String>) {
        val files = File(plannersDirectory, "job").walkTopDown().filter { it.isFile && it.extension == "yml" }.toList()
        for (file in files) {
            if (!jobIds.contains(file.nameWithoutExtension)) {
                continue
            }
            val job = ImmutableJob(Configuration.loadFromFile(file))
            Registries.JOB[job.id] = job
        }
    }

    /**
     * 解码服务端按键配置。
     *
     * @param config 当前服务端 config.yml。
     */
    private fun loadKeyBindings(config: Configuration) {
        val section = config.getConfigurationSection("settings.keybinding.keymapping")
        if (section == null) {
            error("Planners 配置缺少 settings.keybinding.keymapping")
        }
        for (key in section.getKeys(false)) {
            val keySection = section.getConfigurationSection(key)
            if (keySection == null) {
                error("按键配置不是节点: $key")
            }
            val binding = ImmutableKeyBinding(keySection)
            Registries.KEYBINDING[binding.id] = binding
        }
    }

    /**
     * 解码服务端的全部职业路由定义。
     *
     * @param plannersDirectory 服务端 Planners 配置根目录。
     */
    private fun loadRouter(routerConfig: Configuration) {
        val router = ImmutableRouter(routerConfig)
        Registries.ROUTER[router.id] = router
    }

    /**
     * 读取选中路由内全部职业阶段 ID。
     *
     * @param routerConfig 骑士职业路由配置。
     * @return 路由中定义的职业阶段 ID。
     */
    private fun readRouteJobIds(routerConfig: Configuration): Set<String> {
        val result = LinkedHashSet<String>()
        for (key in routerConfig.getKeys(false)) {
            if (key == "__option__") {
                continue
            }
            result.add(key)
        }
        return result
    }

    /**
     * 收集当前职业路由全部阶段实际声明的技能 ID。
     *
     * @param routeJobIds 当前路由的职业阶段 ID。
     * @return 去重后的技能 ID。
     */
    private fun collectRouteSkillIds(routeJobIds: Set<String>): Set<String> {
        val result = LinkedHashSet<String>()
        for (jobId in routeJobIds) {
            val job = Registries.JOB.getOrNull(jobId)
            if (job == null) {
                error("未加载骑士路由职业: $jobId")
            }
            result.addAll(job.skillIds)
        }
        return result
    }

    /**
     * 读取当前职业路由实际绑定的技能树 ID。
     *
     * @param routerConfig 骑士职业路由配置。
     * @param routeJobIds 当前路由的职业阶段 ID。
     * @return 去重后的技能树 ID。
     */
    private fun readRouteSkillTreeIds(routerConfig: Configuration, routeJobIds: Set<String>): Set<String> {
        val result = LinkedHashSet<String>()
        for (jobId in routeJobIds) {
            val route = routerConfig.getConfigurationSection(jobId)
            if (route == null) {
                error("骑士路由缺少职业阶段: $jobId")
            }
            val treeIds = route.getStringList("skill.trees")
            result.addAll(treeIds)
        }
        return result
    }

    /**
     * 从服务端 Planners SQLite 数据库读取 Dev 的实际职业线状态。
     *
     * 该测试只读取 production data.db，不调用 Planners Database，避免测试初始化
     * 额外创建连接池或触发任何持久化写入。
     *
     * @param plannersDirectory 服务端 Planners 数据目录。
     * @return 与生产数据库一致的玩家职业聚合。
     */
    private fun loadProductionPlayerRouter(plannersDirectory: File): PlayerRouter {
        val databaseFile = File(plannersDirectory, "data.db")
        assertTrue(databaseFile.isFile, "未找到服务端 Planners 数据库: $databaseFile")
        Class.forName("org.sqlite.JDBC")
        val connection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath.replace('\\', '/')}")
        try {
            val routerStatement = connection.prepareStatement(
                "select id, user, router, level, experience, current_route, sp_current, sp_used from planners_router order by id limit 1"
            )
            try {
                val routerResult = routerStatement.executeQuery()
                try {
                    if (!routerResult.next()) {
                        error("服务端 Planners 数据库没有玩家职业聚合")
                    }
                    val routerId = routerResult.getString("router")
                    val userId = routerResult.getLong("user")
                    val bindingId = routerResult.getLong("id")
                    val level = routerResult.getInt("level")
                    val experience = routerResult.getInt("experience")
                    val currentRouteId = routerResult.getLong("current_route")
                    val skillPointsCurrent = routerResult.getInt("sp_current")
                    val skillPointsUsed = routerResult.getInt("sp_used")
                    val routeStatement = connection.prepareStatement(
                        "select id, parent, route from planners_route where user = ? and router = ? order by id"
                    )
                    try {
                        routeStatement.setLong(1, userId)
                        routeStatement.setString(2, routerId)
                        val routeResult = routeStatement.executeQuery()
                        try {
                            val routes = ArrayList<PlayerRoute>()
                            while (routeResult.next()) {
                                val routeBindingId = routeResult.getLong("id")
                                val parentId = routeResult.getLong("parent")
                                val jobId = routeResult.getString("route")
                                val skills = loadProductionPlayerSkills(connection, routeBindingId)
                                val nodeStates = loadProductionNodeStates(connection, routeBindingId)
                                routes.add(PlayerRoute(routeBindingId, routerId, parentId, jobId, skills, nodeStates))
                            }
                            if (routes.isEmpty()) {
                                error("服务端 Planners 数据库没有玩家职业阶段")
                            }
                            return PlayerRouter(
                                bindingId,
                                userId,
                                routerId,
                                level,
                                experience,
                                currentRouteId,
                                skillPointsCurrent,
                                skillPointsUsed,
                                routes
                            )
                        } finally {
                            routeResult.close()
                        }
                    } finally {
                        routeStatement.close()
                    }
                } finally {
                    routerResult.close()
                }
            } finally {
                routerStatement.close()
            }
        } finally {
            connection.close()
        }
    }

    /** 从 SQLite 读取一个职业阶段的技能记录。 */
    private fun loadProductionPlayerSkills(connection: Connection, routeId: Long): List<PlayerSkill> {
        val statement = connection.prepareStatement(
            "select id, node, level, equipped, backpack_page, backpack_slot from planners_skill where route = ? order by id"
        )
        try {
            statement.setLong(1, routeId)
            val result = statement.executeQuery()
            try {
                val skills = ArrayList<PlayerSkill>()
                while (result.next()) {
                    val skillId = result.getString("node")
                    val level = result.getInt("level")
                    val equipped = result.getInt("equipped") != 0
                    val page = result.getString("backpack_page")
                    val slot = result.getString("backpack_slot")
                    skills.add(PlayerSkill(result.getLong("id"), skillId, level, equipped, page, slot))
                }
                return skills
            } finally {
                result.close()
            }
        } finally {
            statement.close()
        }
    }

    /** 从 SQLite 读取一个职业阶段的技能树节点状态。 */
    private fun loadProductionNodeStates(connection: Connection, routeId: Long): List<com.gitee.planners.core.player.PlayerSkillTreeNodeState> {
        val statement = connection.prepareStatement(
            "select id, tree, node, level from planners_skill_tree_node where route = ? order by id"
        )
        try {
            statement.setLong(1, routeId)
            val result = statement.executeQuery()
            try {
                val states = ArrayList<com.gitee.planners.core.player.PlayerSkillTreeNodeState>()
                while (result.next()) {
                    states.add(
                        com.gitee.planners.core.player.PlayerSkillTreeNodeState(
                            result.getLong("id"),
                            result.getString("tree"),
                            result.getString("node"),
                            result.getInt("level")
                        )
                    )
                }
                return states
            } finally {
                result.close()
            }
        } finally {
            statement.close()
        }
    }

    /**
     * 创建 MockBukkit 的真实在线玩家。
     *
     * @param server 当前 MockBukkit 服务端。
     * @return Bukkit Player 代理。
     */
    private fun createPlayer(server: ServerMock): Player {
        return server.addPlayer("SkillTreeBenchmark")
    }

    /**
     * 为未参与快照路径的 Bukkit 方法提供 JVM 默认返回值。
     *
     * @param type 方法返回类型。
     * @return 对应零值；对象返回 null。
     */
    private fun defaultValue(type: Class<*>): Any? {
        if (!type.isPrimitive) {
            return null
        }
        if (type == Boolean::class.javaPrimitiveType) {
            return false
        }
        if (type == Char::class.javaPrimitiveType) {
            return '\u0000'
        }
        if (type == Byte::class.javaPrimitiveType) {
            return 0.toByte()
        }
        if (type == Short::class.javaPrimitiveType) {
            return 0.toShort()
        }
        if (type == Int::class.javaPrimitiveType) {
            return 0
        }
        if (type == Long::class.javaPrimitiveType) {
            return 0L
        }
        if (type == Float::class.javaPrimitiveType) {
            return 0F
        }
        if (type == Double::class.javaPrimitiveType) {
            return 0.0
        }
        return null
    }

    /**
     * 加载生产 Zeus 快照脚本及其实际 JavaScript 集合转换逻辑。
     *
     * @param session ScriptEngine 生产 GraalJS 会话。
     */
    private fun installProductionSnapshot(session: ScriptSession) {
        val source = readBundledScript("zeus.planners.js")
        val rewritten = source.replace(
            "var Bukkit = Java.type(\"org.bukkit.Bukkit\");",
            "var Bukkit = { getCurrentTick: function () { return 1; } };"
        )
        checkScriptResult(session.eval(readBundledScript("api.utils.js")), "加载生产 ScriptEngine 转换脚本")
        checkScriptResult(
            session.eval(
            """
                var ZeusJs = {};
            """
            ),
            "初始化 Zeus 快照测试 API"
        )
        checkScriptResult(session.eval(readBundledScript("planners.api.js")), "加载生产 Planners JavaScript API")
        checkScriptResult(session.eval(rewritten), "加载生产 Zeus 快照脚本")
        checkScriptResult(
            session.eval(
            """
                function __productionSkillTreeSnapshotProfile(player, template, playerRouter) {
                    var snapshotStart = java.lang.System.nanoTime();
                    var snapshot = ZeusJs.planners.skillTreeSnapshot(player, template, playerRouter);
                    var snapshotMs = (java.lang.System.nanoTime() - snapshotStart) / 1000000.0;
                    var serializationStart = java.lang.System.nanoTime();
                    var payload = JSON.stringify(snapshot);
                    var serializationMs = (java.lang.System.nanoTime() - serializationStart) / 1000000.0;
                    return JSON.stringify({
                        payloadChars: payload.length,
                        snapshotMs: snapshotMs,
                        serializationMs: serializationMs,
                        profiling: snapshot.profiling
                    });
                }
            """.trimIndent()
            ),
            "声明生产技能树快照统计入口"
        )
    }

    /**
     * 验证生产 ScriptEngine 会话中的脚本执行结果。
     *
     * @param result ScriptEngine 执行结果。
     * @param operation 当前执行步骤。
     */
    private fun checkScriptResult(result: com.gitee.scriptengine.api.ScriptResult, operation: String) {
        if (result.success) {
            return
        }
        val error = result.error
        if (error == null) {
            error("$operation 失败")
        }
        throw IllegalStateException("$operation 失败", error)
    }

    /**
     * 清理当前测试注册的真实 Planners 定义，防止影响其他测试。
     */
    private fun clearRegistries() {
        Registries.JOB.clear()
        Registries.SKILL.clear()
        Registries.SKILL_TREE.clear()
        Registries.ROUTER.clear()
        Registries.KEYBINDING.clear()
    }

    /**
     * 注册最小 TabooLib 平台 IO 服务，使真实 Registries 可以按生产目录完成静态初始化。
     *
     * @param plannersDirectory 当前服务端的 Planners 数据目录。
     */
    private fun installPlatformIo(plannersDirectory: File) {
        val existing = PlatformFactory.getServiceOrNull<PlatformIO>()
        if (existing != null) {
            return
        }
        val handler = InvocationHandler { _, method, arguments ->
            when (method.name) {
                "getPluginId" -> "PlannersIntegrationTest"
                "getPluginVersion" -> "test"
                "isPrimaryThread" -> true
                "getDataFolder" -> plannersDirectory
                "getJarFile" -> File(plannersDirectory, "Planners-test.jar")
                "getPlatformData" -> emptyMap<String, Any>()
                "releaseResourceFile" -> {
                    val path = arguments?.getOrNull(0)?.toString()
                    if (path == null) {
                        null
                    } else {
                        File(plannersDirectory, path)
                    }
                }
                "info", "warning", "severe" -> null
                else -> defaultValue(method.returnType)
            }
        }
        val proxy = Proxy.newProxyInstance(
            PlatformIO::class.java.classLoader,
            arrayOf(PlatformIO::class.java),
            handler
        ) as PlatformIO
        PlatformFactory.registerService(proxy)
    }

    /**
     * 格式化纳秒耗时。
     *
     * @param nanos 纳秒数。
     * @return 两位小数的毫秒字符串。
     */
    private fun formatMs(nanos: Long): String {
        return String.format(java.util.Locale.ROOT, "%.2f", nanos / 1_000_000.0)
    }

    /**
     * 将单调纳秒起点换算为当前已耗毫秒。
     *
     * @param startNanos 单调纳秒起点。
     * @return 已耗毫秒。
     */
    private fun elapsedMs(startNanos: Long): Double {
        return (System.nanoTime() - startNanos) / 1_000_000.0
    }

    /**
     * 解析单次生产快照统计结果。
     *
     * @param value JavaScript 统计入口返回的 JSON。
     * @param invokeMs Java 到 ScriptEngine 的完整调用耗时。
     * @return 可参与聚合的单次样本。
     */
    private fun parseSnapshotSample(value: String, invokeMs: Double): SnapshotSample {
        val root = JsonParser.parseString(value).asJsonObject
        val profile = root.getAsJsonObject("profiling") ?: JsonObject()
        return SnapshotSample(
            invokeMs,
            root.doubleValue("snapshotMs"),
            root.doubleValue("serializationMs"),
            root.get("payloadChars").asInt,
            profile.doubleValue("immutableMs"),
            profile.doubleValue("playerMs"),
            profile.doubleValue("backpackStructureMs"),
            profile.doubleValue("backpackEquippedMs"),
            profile.doubleValue("routeProjectionMs"),
            profile.doubleValue("nodeVerifyMs"),
            profile.doubleValue("treeProjectionMs"),
            profile.doubleValue("nodeDataMs"),
            profile.doubleValue("nodeRuntimeReadMs"),
            profile.doubleValue("nodeSkillDataMs"),
            profile.doubleValue("textRenderMs"),
            profile.doubleValue("variableEvalMs"),
            profile.doubleValue("templateResolveMs"),
            profile.doubleValue("colorizeMs")
        )
    }

    /**
     * 输出完整快照流程的分层平均耗时图。
     *
     * 图表只在同一层级内比较互不包含的阶段；子阶段单独显示，避免重复累加。
     *
     * @param samples 热态样本集合。
     * @param scenario 当前生产场景规模。
     */
    private fun printBenchmarkReport(samples: List<SnapshotSample>, scenario: Scenario, mode: String) {
        val average = SnapshotSample.average(samples)
        val hostBoundaryMs = nonNegative(average.invokeMs - average.snapshotMs - average.serializationMs)
        val snapshotOtherMs = nonNegative(
            average.snapshotMs - average.immutableMs - average.playerMs - average.backpackStructureMs - average.backpackEquippedMs
        )
        val playerOtherMs = nonNegative(average.playerMs - average.routeProjectionMs)
        val immutableOtherMs = nonNegative(average.immutableMs - average.textRenderMs)
        val treeOtherMs = nonNegative(average.treeProjectionMs - average.nodeDataMs)
        val textOtherMs = nonNegative(average.textRenderMs - average.variableEvalMs - average.templateResolveMs - average.colorizeMs)

        println(
            "[SkillTreeProductionSnapshotScenario] " +
                "mode=" + mode +
                " " +
                "iterations=" + samples.size +
                " payloadChars=" + average.payloadChars +
                " routes=" + scenario.routeCount +
                " skills=" + scenario.skillCount +
                " trees=" + scenario.treeCount +
                " nodes=" + scenario.nodeCount
        )
        println("[SkillTreeProductionSnapshotScenario][端到端] 平均 " + formatMilliseconds(average.invokeMs) + "ms")
        printBar("  脚本快照", average.snapshotMs, average.invokeMs)
        printBar("  JSON 序列化", average.serializationMs, average.invokeMs)
        printBar("  Java/JS 边界", hostBoundaryMs, average.invokeMs)
        println("[SkillTreeProductionSnapshotScenario][快照] 平均 " + formatMilliseconds(average.snapshotMs) + "ms")
        printBar("  不可变数据投影", average.immutableMs, average.snapshotMs)
        printBar("  玩家运行时投影", average.playerMs, average.snapshotMs)
        printBar("  背包结构读取", average.backpackStructureMs, average.snapshotMs)
        printBar("  背包装备读取", average.backpackEquippedMs, average.snapshotMs)
        printBar("  其他脚本开销", snapshotOtherMs, average.snapshotMs)
        println("[SkillTreeProductionSnapshotScenario][玩家运行时投影] 平均 " + formatMilliseconds(average.playerMs) + "ms")
        printBar("  路线与技能树投影", average.routeProjectionMs, average.playerMs)
        printBar("  其他玩家标量读取", playerOtherMs, average.playerMs)
        println("[SkillTreeProductionSnapshotScenario][路线与技能树投影] 平均 " + formatMilliseconds(average.routeProjectionMs) + "ms")
        printBar("  节点运行时校验", average.nodeVerifyMs, average.routeProjectionMs)
        printBar("  技能树节点装配", average.treeProjectionMs, average.routeProjectionMs)
        println("[SkillTreeProductionSnapshotScenario][技能树节点装配] 平均 " + formatMilliseconds(average.treeProjectionMs) + "ms")
        printBar("  节点数据创建", average.nodeDataMs, average.treeProjectionMs)
        printBar("  其他树结构读取", treeOtherMs, average.treeProjectionMs)
        println("[SkillTreeProductionSnapshotScenario][不可变数据投影] 平均 " + formatMilliseconds(average.immutableMs) + "ms")
        printBar("  动态图标文本", average.textRenderMs, average.immutableMs)
        printBar("  其他职业/技能投影", immutableOtherMs, average.immutableMs)
        println("[SkillTreeProductionSnapshotScenario][动态图标文本] 平均 " + formatMilliseconds(average.textRenderMs) + "ms")
        printBar("  显示变量计算", average.variableEvalMs, average.textRenderMs)
        printBar("  模板替换", average.templateResolveMs, average.textRenderMs)
        printBar("  颜色转换", average.colorizeMs, average.textRenderMs)
        printBar("  其他图标文本开销", textOtherMs, average.textRenderMs)
        println(
            "[SkillTreeProductionSnapshotScenario][参考] " +
                "节点运行时读取=" + formatMilliseconds(average.nodeRuntimeReadMs) + "ms " +
                "技能节点引用=" + formatMilliseconds(average.nodeSkillDataMs) + "ms"
        )
    }

    /**
     * 输出生产纳秒计时器对快照热路径的增量影响。
     *
     * 空计时器保留同样的 JavaScript 调用结构，但不再读取 Date.now，
     * 两者的端到端差值即为当前监控方案的实际代价。
     *
     * @param productionClockSamples 使用生产纳秒计时器的样本。
     * @param noOpClockSamples 使用空计时器的样本。
     */
    private fun printTimerProbeReport(
        productionClockSamples: List<SnapshotSample>,
        noOpClockSamples: List<SnapshotSample>
    ) {
        val production = SnapshotSample.average(productionClockSamples)
        val noOp = SnapshotSample.average(noOpClockSamples)
        val timerOverheadMs = nonNegative(production.invokeMs - noOp.invokeMs)
        println("[SkillTreeProductionSnapshotScenario][计时器影响] 生产计时=" + formatMilliseconds(production.invokeMs) + "ms")
        printBar("  空计时器业务基线", noOp.invokeMs, production.invokeMs)
        printBar("  Date.now 计时器增量", timerOverheadMs, production.invokeMs)
        println(
            "[SkillTreeProductionSnapshotScenario][空计时器] " +
                "平均=" + formatMilliseconds(noOp.invokeMs) + "ms " +
                "脚本快照=" + formatMilliseconds(noOp.snapshotMs) + "ms " +
                "JSON=" + formatMilliseconds(noOp.serializationMs) + "ms"
        )
    }

    /**
     * 输出一个固定宽度的耗时比例条。
     *
     * @param label 阶段名称。
     * @param valueMs 当前阶段耗时。
     * @param parentMs 当前层级总耗时。
     */
    private fun printBar(label: String, valueMs: Double, parentMs: Double) {
        var ratio = 0.0
        if (parentMs > 0.0) {
            ratio = valueMs / parentMs
        }
        val width = 32
        val filled = (ratio * width).toInt().coerceIn(0, width)
        val bar = "#".repeat(filled) + ".".repeat(width - filled)
        val percent = ratio * 100.0
        println("$label |$bar| ${formatMilliseconds(valueMs)}ms ${formatMilliseconds(percent)}%")
    }

    /**
     * 防止计时器精度造成的微小负值污染图表。
     *
     * @param value 原始耗时。
     * @return 不小于零的耗时。
     */
    private fun nonNegative(value: Double): Double {
        if (value < 0.0) {
            return 0.0
        }
        return value
    }

    /**
     * 格式化毫秒或百分比数值。
     *
     * @param value 待显示数值。
     * @return 两位小数的本地化无关文本。
     */
    private fun formatMilliseconds(value: Double): String {
        return String.format(java.util.Locale.ROOT, "%.2f", value)
    }

    /** 从测试资源读取随仓库提交的生产脚本。 */
    private fun readBundledScript(name: String): String {
        val resource = javaClass.getResourceAsStream("/zeus-script/$name")
        if (resource == null) {
            error("缺少测试资源: /zeus-script/$name")
        }
        val reader = resource.bufferedReader(Charsets.UTF_8)
        try {
            return reader.readText()
        } finally {
            reader.close()
        }
    }

    /** 将测试资源写入临时脚本工作区，供 Zeus DEFAULT workspace 测试使用。 */
    private fun createBundledScriptRoot(): Path {
        val root = Files.createTempDirectory("planners-zeus-script-")
        val scripts = root.resolve("scripts")
        Files.createDirectories(scripts)
        val names = arrayOf("zeus.planners.js", "api.utils.js", "planners.api.js")
        for (name in names) {
            Files.writeString(
                scripts.resolve(name),
                readBundledScript(name),
                Charsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        }
        return root
    }

    /** 删除本次 workspace 测试创建的临时脚本目录。 */
    private fun deleteRecursively(root: Path) {
        if (!Files.exists(root)) {
            return
        }
        val stream = Files.walk(root)
        try {
            val paths = ArrayList<Path>()
            val iterator = stream.iterator()
            while (iterator.hasNext()) {
                paths.add(iterator.next())
            }
            paths.sortWith(Comparator.reverseOrder())
            for (path in paths) {
                Files.deleteIfExists(path)
            }
        } finally {
            stream.close()
        }
    }

    /**
     * 读取性能测试所需的外部 Planners 数据和 GraalJS 运行时目录。
     *
     * 这些是测试输入而非代码依赖，由执行测试的环境通过 JVM 参数显式提供。
     */
    private fun requiredTestPath(propertyName: String): String {
        val value = System.getProperty(propertyName)
        if (value == null || value.isBlank()) {
            error("缺少性能测试路径参数 -D$propertyName=<path>")
        }
        return value
    }

    /**
     * 从 JSON 对象读取数值字段。
     *
     * @param key 字段名。
     * @return 字段对应的 Double 值。
     */
    private fun JsonObject.doubleValue(key: String): Double {
        if (!has(key)) {
            return 0.0
        }
        return get(key).asDouble
    }

    /**
     * 生产对象快照场景。
     */
    private class Scenario(
        val player: Player,
        val template: PlayerTemplate,
        val router: PlayerRouter,
        val routeCount: Int,
        val skillCount: Int,
        val treeCount: Int,
        val nodeCount: Int
    )

    /**
     * 单次生产快照的分层耗时样本。
     */
    private class SnapshotSample(
        val invokeMs: Double,
        val snapshotMs: Double,
        val serializationMs: Double,
        val payloadChars: Int,
        val immutableMs: Double,
        val playerMs: Double,
        val backpackStructureMs: Double,
        val backpackEquippedMs: Double,
        val routeProjectionMs: Double,
        val nodeVerifyMs: Double,
        val treeProjectionMs: Double,
        val nodeDataMs: Double,
        val nodeRuntimeReadMs: Double,
        val nodeSkillDataMs: Double,
        val textRenderMs: Double,
        val variableEvalMs: Double,
        val templateResolveMs: Double,
        val colorizeMs: Double
    ) {

        companion object {

            /**
             * 计算全部样本的逐字段平均值。
             *
             * @param samples 已完成的热态样本。
             * @return 汇总平均样本。
             */
            fun average(samples: List<SnapshotSample>): SnapshotSample {
                if (samples.isEmpty()) {
                    error("没有可统计的技能树快照样本")
                }
                var invokeMs = 0.0
                var snapshotMs = 0.0
                var serializationMs = 0.0
                var payloadChars = 0
                var immutableMs = 0.0
                var playerMs = 0.0
                var backpackStructureMs = 0.0
                var backpackEquippedMs = 0.0
                var routeProjectionMs = 0.0
                var nodeVerifyMs = 0.0
                var treeProjectionMs = 0.0
                var nodeDataMs = 0.0
                var nodeRuntimeReadMs = 0.0
                var nodeSkillDataMs = 0.0
                var textRenderMs = 0.0
                var variableEvalMs = 0.0
                var templateResolveMs = 0.0
                var colorizeMs = 0.0
                for (sample in samples) {
                    invokeMs += sample.invokeMs
                    snapshotMs += sample.snapshotMs
                    serializationMs += sample.serializationMs
                    payloadChars += sample.payloadChars
                    immutableMs += sample.immutableMs
                    playerMs += sample.playerMs
                    backpackStructureMs += sample.backpackStructureMs
                    backpackEquippedMs += sample.backpackEquippedMs
                    routeProjectionMs += sample.routeProjectionMs
                    nodeVerifyMs += sample.nodeVerifyMs
                    treeProjectionMs += sample.treeProjectionMs
                    nodeDataMs += sample.nodeDataMs
                    nodeRuntimeReadMs += sample.nodeRuntimeReadMs
                    nodeSkillDataMs += sample.nodeSkillDataMs
                    textRenderMs += sample.textRenderMs
                    variableEvalMs += sample.variableEvalMs
                    templateResolveMs += sample.templateResolveMs
                    colorizeMs += sample.colorizeMs
                }
                val count = samples.size.toDouble()
                return SnapshotSample(
                    invokeMs / count,
                    snapshotMs / count,
                    serializationMs / count,
                    (payloadChars / count).toInt(),
                    immutableMs / count,
                    playerMs / count,
                    backpackStructureMs / count,
                    backpackEquippedMs / count,
                    routeProjectionMs / count,
                    nodeVerifyMs / count,
                    treeProjectionMs / count,
                    nodeDataMs / count,
                    nodeRuntimeReadMs / count,
                    nodeSkillDataMs / count,
                    textRenderMs / count,
                    variableEvalMs / count,
                    templateResolveMs / count,
                    colorizeMs / count
                )
            }
        }
    }
}
