package com.gitee.planners.core.player

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.player.PlayerSkillTreeNodeEvent
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeNode
import com.gitee.planners.core.config.SkillTreeSkillNode
import com.gitee.planners.core.database.Database
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
) {

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
                error("PlayerRoute '$bindingId' 存在重复技能树节点状态: $key")
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
                    error("PlayerRoute '$bindingId' 找不到技能树 '$treeId'")
                }
                result.add(tree)
            }
            return result
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
        val state = getNodeStateOrNull(treeId, nodeId)
        if (state == null) {
            return 0
        }
        return state.level
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

    fun canAdvanceNode(player: Player, treeId: String, nodeId: String): ConditionEvaluator.VerifyResult {
        val tree = getTree(treeId)
        val node = getNode(tree, nodeId)
        val currentLevel = getNodeLevel(treeId, nodeId)
        if (currentLevel >= node.maxLevel) {
            return ConditionEvaluator.VerifyResult(false, listOf("节点已满级"))
        }
        val requirements = tree.graph[nodeId] ?: emptyList()
        for (requirement in requirements) {
            val actualLevel = getNodeLevel(treeId, requirement.nodeId)
            if (actualLevel < requirement.minLevel) {
                return ConditionEvaluator.VerifyResult(
                    false,
                    listOf("前置节点 ${requirement.nodeId} 需要 Lv${requirement.minLevel}")
                )
            }
        }
        val targetLevel = currentLevel + 1
        val conditions = node.levels[targetLevel]
        if (conditions == null) {
            return ConditionEvaluator.VerifyResult(false, listOf("节点未定义 Lv$targetLevel 条件"))
        }
        return evaluator.verify(conditions, player, createConditionContext(treeId, nodeId, targetLevel))
    }

    /**
     * 一次性校验当前职业阶段的全部技能树节点。
     *
     * 静态图关系在配置解码时已经验证；此处只检查玩家当前节点等级、前置节点状态和
     * 动态条件。所有树的动态条件请求合并到同一个 GraalJS 会话中执行。
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
        val profiling = SkillTreeRuntimeProjection.Profiling()
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
                val requirements = tree.graph[nodeId] ?: emptyList()
                var requirementHint: String? = null
                for (requirement in requirements) {
                    val requirementStateReadStart = System.nanoTime()
                    val actualLevel = getNodeLevel(treeId, requirement.nodeId)
                    profiling.nodeStateReadNanos += System.nanoTime() - requirementStateReadStart
                    if (actualLevel < requirement.minLevel) {
                        requirementHint = "前置节点 ${requirement.nodeId} 需要 Lv${requirement.minLevel}"
                        break
                    }
                }
                profiling.graphCheckNanos += System.nanoTime() - graphCheckStart
                if (requirementHint != null) {
                    treeResults[nodeId] = ConditionEvaluator.VerifyResult(false, listOf(requirementHint))
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
                error("技能树节点校验结果缺少目标: $requestId")
            }
            val treeResults = results[target.treeId]
            if (treeResults == null) {
                error("技能树节点校验结果缺少技能树: ${target.treeId}")
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
     * 一次性生成当前职业阶段的技能树运行时投影。
     *
     * 此方法在 Java/Kotlin 侧合并节点等级与校验结果，避免脚本侧按节点重复跨语言读取
     * 节点状态、校验 Map 和 VerifyResult。
     *
     * @param player 当前玩家。
     * @return 按技能树及节点配置顺序组织的运行时投影。
     */
    fun getSkillTreeRuntimeProjection(player: Player): SkillTreeRuntimeProjection {
        val totalStart = System.nanoTime()
        val checkProjection = getSkillTreeCheckProjection(player)
        val checkResults = checkProjection.results
        val profiling = checkProjection.profiling
        val trees = ArrayList<SkillTreeRuntimeProjection.Tree>()
        for (tree in skillTrees) {
            val treeBuildStart = System.nanoTime()
            val treeId = tree.id
            val treeChecks = checkResults[treeId]
            if (treeChecks == null) {
                error("技能树运行时投影缺少校验结果: $treeId")
            }
            val levels = IntArray(tree.nodes.size)
            val canAdvanceStates = BooleanArray(tree.nodes.size)
            val hints = ArrayList<List<String>>()
            var nodeIndex = 0
            for ((nodeId, _) in tree.nodes) {
                val check = treeChecks[nodeId]
                if (check == null) {
                    error("技能树运行时投影缺少节点校验结果: $treeId/$nodeId")
                }
                val level = getNodeLevel(treeId, nodeId)
                levels[nodeIndex] = level
                canAdvanceStates[nodeIndex] = check.passed
                hints.add(check.hints)
                nodeIndex += 1
            }
            trees.add(SkillTreeRuntimeProjection.Tree(levels, canAdvanceStates, hints))
            profiling.treeBuildNanos += System.nanoTime() - treeBuildStart
        }
        profiling.totalNanos = System.nanoTime() - totalStart
        return SkillTreeRuntimeProjection(trees, profiling)
    }

    fun advanceNode(player: Player, treeId: String, nodeId: String): CompletableFuture<Void> {
        val tree = getTree(treeId)
        val node = getNode(tree, nodeId)
        val stateKey = nodeStateKey(treeId, nodeId)
        if (!pendingNodeAdvancements.add(stateKey)) {
            throw IllegalStateException("节点正在激活中")
        }
        try {
            return advanceNodeInternal(player, tree, node, stateKey)
        } catch (exception: Exception) {
            pendingNodeAdvancements.remove(stateKey)
            throw exception
        }
    }

    private fun advanceNodeInternal(
        player: Player,
        tree: ImmutableSkillTree,
        node: SkillTreeNode,
        stateKey: String
    ): CompletableFuture<Void> {
        val treeId = tree.id
        val nodeId = node.id
        val currentLevel = getNodeLevel(treeId, nodeId)
        val verification = canAdvanceNode(player, treeId, nodeId)
        if (!verification.passed) {
            throw IllegalStateException(verification.hints.joinToString(", "))
        }
        val targetLevel = currentLevel + 1
        val conditions = node.levels[targetLevel]
        if (conditions == null) {
            throw IllegalStateException("节点未定义 Lv$targetLevel 条件")
        }
        evaluator.consume(conditions, player, createConditionContext(treeId, nodeId, targetLevel))
        val existingState = getNodeStateOrNull(treeId, nodeId)
        if (existingState != null) {
            existingState.level = targetLevel
            Database.INSTANCE.updateSkillTreeNodeState(existingState)
            val future = ensureSkillRecord(player, node).thenApply<Void> {
                PlayerSkillTreeNodeEvent(player.plannersTemplate, this, tree, node, currentLevel, targetLevel).call()
                null
            }
            return future.whenComplete { _, _ ->
                pendingNodeAdvancements.remove(stateKey)
            }
        }
        val future = Database.INSTANCE.createSkillTreeNodeState(this, treeId, nodeId, targetLevel).thenCompose { createdState ->
            nodeStatesByKey[nodeStateKey(treeId, nodeId)] = createdState
            ensureSkillRecord(player, node).thenApply<Void> {
                PlayerSkillTreeNodeEvent(player.plannersTemplate, this, tree, node, currentLevel, targetLevel).call()
                null
            }
        }
        return future.whenComplete { _, _ ->
            pendingNodeAdvancements.remove(stateKey)
        }
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
        return getSkillTreeOrNull(treeId) ?: error("职业 '$jobId' 未绑定技能树 '$treeId'")
    }

    private fun getNode(tree: ImmutableSkillTree, nodeId: String): SkillTreeNode {
        return tree.nodes[nodeId] ?: error("技能树 '${tree.id}' 不存在节点 '$nodeId'")
    }

    private fun createConditionContext(treeId: String, nodeId: String, targetLevel: Int): Map<String, Any> {
        val result = LinkedHashMap<String, Any>()
        result["treeId"] = treeId
        result["nodeId"] = nodeId
        result["nodeLevel"] = targetLevel
        return result
    }

    private fun nodeStateKey(treeId: String, nodeId: String): String {
        return "$treeId:$nodeId"
    }

    private fun createCheckRequestId(treeId: String, nodeId: String): String {
        return "$treeId\u0000$nodeId"
    }

    private class SkillTreeCheckTarget(val treeId: String, val nodeId: String)

    private class SkillTreeCheckProjection(
        val results: Map<String, Map<String, ConditionEvaluator.VerifyResult>>,
        val profiling: SkillTreeRuntimeProjection.Profiling
    ) {
    }
}
