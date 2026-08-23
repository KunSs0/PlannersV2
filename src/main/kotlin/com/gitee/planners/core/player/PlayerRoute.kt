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
}
