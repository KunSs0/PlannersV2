package com.gitee.planners.core.player

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.player.PlayerSkillTreeNodeEvent
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.condition.ConditionSession
import com.gitee.planners.core.condition.ConditionMainThread
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeNode
import com.gitee.planners.core.config.SkillTreeSkillNode
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.script.proxy.ProxyPlayerRoute
import com.gitee.planners.core.script.proxy.ProxyNodeInfo
import com.gitee.planners.core.script.proxy.ProxyRouteTarget
import com.gitee.planners.core.script.proxy.ProxyTreeDefinition
import com.gitee.planners.core.script.proxy.ProxyTreeDefinitionView
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class PlayerRoute(
    val bindingId: Long,
    val routerId: String,
    val parentId: Long,
    val jobId: String,
    skills: List<PlayerSkill>,
    nodeStates: List<PlayerSkillTreeNodeState>
) : ProxyRouteTarget<Player> {

    val router: ImmutableRouter
        get() = Registries.ROUTER.getOrNull(routerId) ?: error("Could not find router with id '$routerId'")

    private val route: ImmutableRoute
        get() = router.getRouteOrNull(jobId) ?: error("Couldn't find route '$jobId' in router '$routerId'")

    @get:JvmName("job0")
    private val job: ImmutableJob
        get() = Registries.JOB.getOrNull(jobId) ?: error("Couldn't find job with id '$jobId'")

    private val skillsById = LinkedHashMap<String, PlayerSkill>()
    private val nodeStatesByKey = LinkedHashMap<String, PlayerSkillTreeNodeState>()
    private val pendingNodeAdvancements = ConcurrentHashMap.newKeySet<String>()
    private val evaluator = ConditionEvaluator()

    init {
        for (skill in skills) {
            skillsById[skill.id] = skill
        }
        for (state in nodeStates) {
            val key = nodeStateKey(state.treeId, state.nodeId)
            if (nodeStatesByKey.containsKey(key)) {
            error("PlayerRoute '$bindingId' contains a duplicate skill-tree node state: $key")
            }
            nodeStatesByKey[key] = state
        }
    }

    val name: String
        get() = job.name

    val skillTrees: List<ImmutableSkillTree>
        get() {
            val result = ArrayList<ImmutableSkillTree>()
            for (treeId in route.skillTreeIds) {
                val tree = Registries.SKILL_TREE.getOrNull(treeId)
                if (tree == null) {
                    error("PlayerRoute '$bindingId' cannot find skill tree '$treeId'")
                }
                result.add(tree)
            }
            return result
        }

    override val proxySkillTrees: List<ProxyTreeDefinition>
        get() {
            val result = ArrayList<ProxyTreeDefinition>()
            for (tree in skillTrees) {
                result.add(ProxyTreeDefinitionView(tree))
            }
            return result
        }

    /**
     * 生成后台脚本快照；同一代理的全部节点复用一份外部条件数据。
     * 存在异步条件时须在快照工作线程调用，主线程使用 preparePlayerRoute 异步获取代理。
     */
    fun getPlayerRoute(player: Player): ProxyPlayerRoute<Player> {
        val session = evaluator.prepareBlocking(conditionKeys(), player)
        return ProxyPlayerRoute(SnapshotTarget(session), player)
    }

    /** 异步准备当前节点的外部条件数据并生成代理，供主线程调用方使用。 */
    fun preparePlayerRoute(player: Player): CompletableFuture<ProxyPlayerRoute<Player>> {
        return evaluator.prepare(conditionKeys(), player.uniqueId).thenApply { session ->
            ProxyPlayerRoute(SnapshotTarget(session), player)
        }
    }

    /** 收集本职业当前可升级等级所引用的模板，避免查询未使用的可选适配。 */
    private fun conditionKeys(): Set<String> {
        val keys = LinkedHashSet<String>()
        for (tree in skillTrees) {
            for (node in tree.nodes.values) {
                val conditions = node.levels[getNodeLevel(tree.id, node.id) + 1]
                if (conditions != null) {
                    keys.addAll(conditions.keys)
                }
            }
        }
        return keys
    }

    /** 将请求快照绑定到同步脚本代理，不将玩家数据写入全局缓存。 */
    private inner class SnapshotTarget(private val session: ConditionSession) : ProxyRouteTarget<Player> {
        override val proxySkillTrees: List<ProxyTreeDefinition>
            get() = this@PlayerRoute.proxySkillTrees

        /** 在同一外部数据快照上计算节点信息。 */
        override fun getNodeInfo(player: Player, treeId: String, nodeId: String): ProxyNodeInfo {
            val result = canAdvanceNode(player, treeId, nodeId, session)
            return ProxyNodeInfo(treeId, nodeId, getNodeLevel(treeId, nodeId), result.passed, result.hints)
        }
    }

    fun getSkillTreeOrNull(treeId: String): ImmutableSkillTree? {
        for (tree in skillTrees) {
            if (tree.id == treeId) {
                return tree
            }
        }
        return null
    }

    fun getBranches(): List<ImmutableRoute> {
        return route.getBranches()
    }

    fun getJob(): ImmutableJob {
        return job
    }

    fun getIcon(): ItemStack? {
        return route.getIcon()
    }

    fun getRegisteredSkill(): Map<String, PlayerSkill> {
        return skillsById
    }

    fun registerSkill(skill: PlayerSkill) {
        skillsById[skill.id] = skill
    }

    fun getSkillOrNull(id: String): PlayerSkill? {
        return skillsById[id]
    }

    fun hasSkill(id: String): Boolean {
        return skillsById.containsKey(id)
    }

    fun getImmutableSkillValues(): List<ImmutableSkill> {
        return job.getImmutableSkillValues()
    }

    fun getImmutableSkill(id: String): ImmutableSkill? {
        return job.getSkillOrNull(id)
    }

    fun hasImmutableSkill(id: String): Boolean {
        return job.hasSkill(id)
    }

    fun getNodeStateOrNull(treeId: String, nodeId: String): PlayerSkillTreeNodeState? {
        return nodeStatesByKey[nodeStateKey(treeId, nodeId)]
    }

    fun getNodeLevel(treeId: String, nodeId: String): Int {
        val state = nodeStatesByKey[nodeStateKey(treeId, nodeId)]
        if (state == null) {
            return 0
        }
        return state.level
    }

    override fun getNodeInfo(player: Player, treeId: String, nodeId: String): ProxyNodeInfo {
        val level = getNodeLevel(treeId, nodeId)
        val verification = canAdvanceNode(player, treeId, nodeId)
        return ProxyNodeInfo(treeId, nodeId, level, verification.passed, verification.hints)
    }

    fun getNodeLevels(treeId: String): Map<String, Int> {
        val result = LinkedHashMap<String, Int>()
        for (state in nodeStatesByKey.values) {
            if (state.treeId == treeId) {
                result[state.nodeId] = state.level
            }
        }
        return result
    }

    fun getNodeStates(): Collection<PlayerSkillTreeNodeState> {
        return nodeStatesByKey.values
    }

    /** 校验节点图关系和目标等级条件；学习调用方须传入异步准备完成的 session。 */
    fun canAdvanceNode(player: Player, treeId: String, nodeId: String, session: ConditionSession? = null): ConditionEvaluator.VerifyResult {
        val tree = getTree(treeId)
        val node = getNode(tree, nodeId)
        val currentLevel = getNodeLevel(treeId, nodeId)
        if (currentLevel >= node.maxLevel) {
            return ConditionEvaluator.VerifyResult(false, listOf("节点已满级"))
        }
        val graphResult = verifyGraph(tree, treeId, nodeId)
        if (!graphResult.passed) {
            return graphResult
        }
        val targetLevel = currentLevel + 1
        val conditions = node.levels[targetLevel]
        if (conditions == null) {
            return ConditionEvaluator.VerifyResult(false, listOf("节点未定义 Lv$targetLevel 条件"))
        }
        return evaluator.verify(conditions, player, createConditionContext(treeId, nodeId, targetLevel), session)
    }

    /**
     * 一次性校验当前职业阶段的全部技能树节点。
     *
     * 静态图关系在配置解码时已经验证；此处只检查玩家当前节点等级、前置节点状态和
     * 动态条件。所有树的动态条件请求合并到同一个 Nova 串行作用域中执行。
     *
     * @param player 当前玩家。
     * @return 技能树 ID 到节点校验结果的映射。
     */
    fun getSkillTreeCheckResults(player: Player): Map<String, Map<String, ConditionEvaluator.VerifyResult>> {
        return getSkillTreeCheckProjection(player).results
    }

    /**
     * 生成全部节点校验结果与本次校验的分段统计。
     *
     * 动态条件仍然只在一次批处理调用中执行；本方法只记录各段实际耗时，不改变校验语义。
     */
    private fun getSkillTreeCheckProjection(player: Player): SkillTreeCheckProjection {
        val results = LinkedHashMap<String, LinkedHashMap<String, ConditionEvaluator.VerifyResult>>()
        val requests = ArrayList<ConditionEvaluator.VerifyRequest>()
        val requestTargets = LinkedHashMap<String, SkillTreeCheckTarget>()
        val profiling = SkillTreeCheckProfiling()
        for (tree in skillTrees) {
            val treeId = tree.id
            val treeResults = LinkedHashMap<String, ConditionEvaluator.VerifyResult>()
            results[treeId] = treeResults
            for ((nodeId, node) in tree.nodes) {
                val stateReadStart = System.nanoTime()
                val currentLevel = getNodeLevel(treeId, nodeId)
                profiling.nodeStateReadNanos += System.nanoTime() - stateReadStart
                if (currentLevel >= node.maxLevel) {
                    treeResults[nodeId] = ConditionEvaluator.VerifyResult(false, listOf("节点已满级"))
                    continue
                }
                val graphCheckStart = System.nanoTime()
                val graphResult = verifyGraph(tree, treeId, nodeId)
                profiling.graphCheckNanos += System.nanoTime() - graphCheckStart
                if (!graphResult.passed) {
                    treeResults[nodeId] = graphResult
                    continue
                }
                val targetLevel = currentLevel + 1
                val conditions = node.levels[targetLevel]
                if (conditions == null) {
                    treeResults[nodeId] = ConditionEvaluator.VerifyResult(false, listOf("节点未定义 Lv$targetLevel 条件"))
                    continue
                }
                val requestBuildStart = System.nanoTime()
                val requestId = createCheckRequestId(treeId, nodeId)
                requests.add(
                    ConditionEvaluator.VerifyRequest(
                        requestId,
                        conditions,
                        createConditionContext(treeId, nodeId, targetLevel)
                    )
                )
                requestTargets[requestId] = SkillTreeCheckTarget(treeId, nodeId)
                profiling.requestBuildNanos += System.nanoTime() - requestBuildStart
            }
        }
        val conditionVerifyStart = System.nanoTime()
        val batchVerification = evaluator.verifyAllProfiled(requests, player)
        profiling.conditionVerifyNanos += System.nanoTime() - conditionVerifyStart
        profiling.conditionProfiling = batchVerification.profiling
        val resultApplyStart = System.nanoTime()
        for ((requestId, verification) in batchVerification.results) {
            val target = requestTargets[requestId]
            if (target == null) {
                error("Skill-tree node verification is missing target: $requestId")
            }
            val treeResults = results[target.treeId]
            if (treeResults == null) {
                error("Skill-tree node verification is missing tree: ${target.treeId}")
            }
            treeResults[target.nodeId] = verification
        }
        profiling.resultApplyNanos += System.nanoTime() - resultApplyStart
        val exposedResults = LinkedHashMap<String, Map<String, ConditionEvaluator.VerifyResult>>()
        for ((treeId, treeResults) in results) {
            exposedResults[treeId] = treeResults
        }
        return SkillTreeCheckProjection(exposedResults, profiling)
    }

    /**
     * 主线程锁定本节点，异步准备外部数据，回主线程再次校验后才消耗与升级。
     * 等待期间重复点击会失败；任意成功或失败路径都释放本节点的请求锁。
     */
    fun advanceNode(player: Player, treeId: String, nodeId: String): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(Unit).thenComposeAsync({
            val tree = getTree(treeId)
            val node = getNode(tree, nodeId)
            val stateKey = nodeStateKey(treeId, nodeId)
            check(pendingNodeAdvancements.add(stateKey)) { "该技能树节点正在升级" }
            try {
                val level = getNodeLevel(treeId, nodeId)
                val conditions = node.levels[level + 1]
                require(conditions != null) { "节点已满级或未定义下一级条件" }
                val future = evaluator.prepare(conditions.keys, player.uniqueId).thenComposeAsync({ session ->
                    // 等待查库期间玩家可能切换职业、断线、重载配置或由其他操作修改节点。
                    check(player.isOnline) { "玩家已离线" }
                    val router = player.plannersTemplate.playerRouter
                    check(router != null && router.currentRoute === this) { "当前职业已经改变" }
                    check(getTree(treeId) === tree && getNode(tree, nodeId) === node) { "技能树配置已经改变" }
                    check(getNodeLevel(treeId, nodeId) == level) { "节点等级已经改变，请重新学习" }
                    advanceNodeInternal(player, tree, node, session)
                }, ConditionMainThread)
                future.whenComplete { _, _ ->
                    pendingNodeAdvancements.remove(stateKey)
                }
            } catch (exception: Exception) {
                pendingNodeAdvancements.remove(stateKey)
                CompletableFuture.failedFuture<Void>(exception)
            }
        }, ConditionMainThread)
    }

    /** 已回到主线程，在同一请求快照上重检条件并执行一次消耗。 */
    private fun advanceNodeInternal(
        player: Player,
        tree: ImmutableSkillTree,
        node: SkillTreeNode,
        session: ConditionSession
    ): CompletableFuture<Void> {
        val treeId = tree.id
        val nodeId = node.id
        val currentLevel = getNodeLevel(treeId, nodeId)
        val verification = canAdvanceNode(player, treeId, nodeId, session)
        if (!verification.passed) {
            throw IllegalStateException(verification.hints.joinToString(", "))
        }
        val targetLevel = currentLevel + 1
        val conditions = node.levels[targetLevel]
        if (conditions == null) {
            throw IllegalStateException("The skill-tree node does not define conditions for level $targetLevel")
        }
        evaluator.consume(conditions, player, createConditionContext(treeId, nodeId, targetLevel), session)
        val existingState = getNodeStateOrNull(treeId, nodeId)
        if (existingState != null) {
            existingState.level = targetLevel
            Database.INSTANCE.updateSkillTreeNodeState(existingState)
            val future = ensureSkillRecord(player, node).thenApplyAsync<Void>({
                PlayerSkillTreeNodeEvent(player.plannersTemplate, this, tree, node, currentLevel, targetLevel).call()
                null
            }, ConditionMainThread)
            return future
        }
        val future = Database.INSTANCE.createSkillTreeNodeState(this, treeId, nodeId, targetLevel).thenComposeAsync({ createdState ->
            nodeStatesByKey[nodeStateKey(treeId, nodeId)] = createdState
            ensureSkillRecord(player, node).thenApplyAsync<Void>({
                PlayerSkillTreeNodeEvent(player.plannersTemplate, this, tree, node, currentLevel, targetLevel).call()
                null
            }, ConditionMainThread)
        }, ConditionMainThread)
        return future
    }

    private fun ensureSkillRecord(player: Player, node: SkillTreeNode): CompletableFuture<PlayerSkill?> {
        if (node !is SkillTreeSkillNode) {
            return CompletableFuture.completedFuture(null)
        }
        val existing = getSkillOrNull(node.skillId)
        if (existing != null) {
            return CompletableFuture.completedFuture(existing)
        }
        return player.plannersTemplate.getSkill(node.skillId).thenApply { it }
    }

    private fun getTree(treeId: String): ImmutableSkillTree {
        return getSkillTreeOrNull(treeId) ?: error("Job '$jobId' is not bound to skill tree '$treeId'")
    }

    private fun getNode(tree: ImmutableSkillTree, nodeId: String): SkillTreeNode {
        return tree.nodes[nodeId] ?: error("Skill tree '${tree.id}' does not contain node '$nodeId'")
    }

    private fun createConditionContext(treeId: String, nodeId: String, targetLevel: Int): Map<String, Any> {
        val result = LinkedHashMap<String, Any>()
        result["treeId"] = treeId
        result["nodeId"] = nodeId
        result["nodeLevel"] = targetLevel
        return result
    }

    private fun verifyGraph(tree: ImmutableSkillTree, treeId: String, nodeId: String): ConditionEvaluator.VerifyResult {
        val graph = tree.graph[nodeId]
        if (graph == null) {
            return ConditionEvaluator.VerifyResult(true, emptyList())
        }
        var firstHint: String? = null
        for (group in graph.groups.values) {
            var passed = false
            if (group.mode == ImmutableSkillTree.Graph.Mode.ALL) {
                passed = true
                for ((requirementNodeId, minLevel) in group.requirements) {
                    val actualLevel = getNodeLevel(treeId, requirementNodeId)
                    if (actualLevel < minLevel) {
                        passed = false
                        if (firstHint == null) {
                            firstHint = "前置节点 $requirementNodeId 需要 Lv$minLevel"
                        }
                        break
                    }
                }
            } else {
                for ((requirementNodeId, minLevel) in group.requirements) {
                    val actualLevel = getNodeLevel(treeId, requirementNodeId)
                    if (actualLevel >= minLevel) {
                        passed = true
                        break
                    }
                    if (firstHint == null) {
                        firstHint = "前置节点 $requirementNodeId 需要 Lv$minLevel"
                    }
                }
            }
            if (passed) {
                return ConditionEvaluator.VerifyResult(true, emptyList())
            }
        }
        if (firstHint == null) {
            return ConditionEvaluator.VerifyResult(false, listOf("前置条件未满足"))
        }
        return ConditionEvaluator.VerifyResult(false, listOf(firstHint))
    }

    private fun nodeStateKey(treeId: String, nodeId: String): String {
        return "$treeId:$nodeId"
    }

    private fun createCheckRequestId(treeId: String, nodeId: String): String {
        return "$treeId\u0000$nodeId"
    }

    private class SkillTreeCheckTarget(val treeId: String, val nodeId: String)

    /** 节点校验批处理的分段统计（纳秒累加）。 */
    class SkillTreeCheckProfiling {

        var nodeStateReadNanos: Long = 0
        var graphCheckNanos: Long = 0
        var requestBuildNanos: Long = 0
        var conditionVerifyNanos: Long = 0
        var resultApplyNanos: Long = 0
        var totalNanos: Long = 0
        var conditionProfiling: ConditionEvaluator.BatchProfiling? = null

        /** 将统计压缩为服务器日志中的一段文本。 */
        fun toLogText(): String {
            val text = StringBuilder()
            text.append("skillTreeUs={total=")
            text.append(formatMicros(totalNanos))
            text.append(",nodeState=")
            text.append(formatMicros(nodeStateReadNanos))
            text.append(",graph=")
            text.append(formatMicros(graphCheckNanos))
            text.append(",request=")
            text.append(formatMicros(requestBuildNanos))
            text.append(",condition=")
            text.append(formatMicros(conditionVerifyNanos))
            text.append(",apply=")
            text.append(formatMicros(resultApplyNanos))
            text.append("}")
            val profiling = conditionProfiling
            if (profiling != null) {
                text.append(" ")
                text.append(profiling.toLogText())
            }
            return text.toString()
        }

        private fun formatMicros(nanos: Long): String {
            return (nanos / 1_000L).toString()
        }
    }

    private class SkillTreeCheckProjection(
        val results: Map<String, Map<String, ConditionEvaluator.VerifyResult>>,
        val profiling: SkillTreeCheckProfiling
    ) {
    }
}
