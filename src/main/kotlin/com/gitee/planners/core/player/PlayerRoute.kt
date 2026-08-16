package com.gitee.planners.core.player

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.concurrent.CompletableFuture

/** 玩家转职线中的单个 Job 阶段。 */
class PlayerRoute(
    val bindingId: Long,
    val routerId: String,
    val parentId: Long,
    val jobId: String,
    skills: List<PlayerSkill>
) {

    val router: ImmutableRouter
        get() = Registries.ROUTER.getOrNull(routerId) ?: error("Could not find router with id '$routerId'")

    private val route: ImmutableRoute
        get() = router.getRouteOrNull(jobId) ?: error("Couldn't find route '$jobId' in router '$routerId'")

    private val job: ImmutableJob
        @JvmName("job0")
        get() = Registries.JOB.getOrNull(jobId) ?: error("Couldn't find job with id '$jobId'")

    private val skillsById = LinkedHashMap<String, PlayerSkill>()

    init {
        for (skill in skills) {
            skillsById[skill.id] = skill
        }
    }

    val name: String
        get() = job.name

    val skillTree: SkillTree?
        get() {
            val skillTreeId = route.skillTree
            if (skillTreeId == null) {
                return null
            }
            val immutable = Registries.SKILL_TREE.getOrNull(skillTreeId)
            if (immutable == null) {
                return null
            }
            return SkillTree(immutable)
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

    inner class SkillTree(
        val immutable: ImmutableSkillTree
    ) {
        private val evaluator = ConditionEvaluator()

        val treeId: String
            get() = immutable.id

        fun getLevel(skillId: String): Int {
            return skillsById[skillId]?.level ?: 0
        }

        fun getLearnedSkills(): Map<String, PlayerSkill> {
            return skillsById.filterKeys { immutable.nodes.containsKey(it) }
        }

        fun learn(player: Player, skillId: String): CompletableFuture<Void> {
            val existing = skillsById[skillId]
            if (existing != null && existing.level > 0) {
                throw IllegalStateException("技能 $skillId 已学习")
            }
            val node = immutable.nodes[skillId]
            if (node == null) {
                throw IllegalArgumentException("技能树 '${immutable.id}' 中不存在技能 '$skillId'")
            }
            val conditions = node.levels[1]
            if (conditions == null) {
                throw IllegalArgumentException("技能 '$skillId' 未定义 Lv1 条件")
            }
            val result = evaluator.verify(conditions, player)
            if (!result.passed) {
                throw IllegalStateException("不满足学习条件: ${result.hints.joinToString(", ")}")
            }
            evaluator.consume(conditions, player)

            val template = player.plannersTemplate
            if (existing != null) {
                PlayerTemplateAPI.setSkillLevel(template, existing, 1)
                return CompletableFuture.completedFuture(null)
            }
            val immutableSkill = getImmutableSkill(skillId)
            if (immutableSkill == null) {
                throw IllegalArgumentException("ImmutableSkill '$skillId' 不存在")
            }
            return Database.INSTANCE.createPlayerSkill(template, this@PlayerRoute, immutableSkill).thenApply { playerSkill ->
                registerSkill(playerSkill)
                val playerRouter = template.playerRouter
                if (playerRouter != null) {
                    playerRouter.updateEquippedIndex(playerSkill)
                }
                PlayerTemplateAPI.setSkillLevel(template, playerSkill, 1)
                null
            }
        }

        fun upgrade(player: Player, skillId: String): CompletableFuture<Void> {
            val playerSkill = skillsById[skillId]
            if (playerSkill == null) {
                throw IllegalStateException("技能 $skillId 未学习")
            }
            val node = immutable.nodes[skillId]
            if (node == null) {
                throw IllegalArgumentException("技能树中不存在技能 '$skillId'")
            }
            if (playerSkill.level >= node.maxLevel) {
                throw IllegalStateException("技能 $skillId 已满级 (${playerSkill.level}/${node.maxLevel})")
            }
            val targetLevel = playerSkill.level + 1
            val conditions = node.levels[targetLevel]
            if (conditions == null) {
                throw IllegalArgumentException("技能 '$skillId' 未定义 Lv$targetLevel 条件")
            }
            val result = evaluator.verify(conditions, player)
            if (!result.passed) {
                throw IllegalStateException("不满足升级条件: ${result.hints.joinToString(", ")}")
            }
            evaluator.consume(conditions, player)
            PlayerTemplateAPI.setSkillLevel(player.plannersTemplate, playerSkill, targetLevel)
            return CompletableFuture.completedFuture(null)
        }

        fun canLearn(player: Player, skillId: String): ConditionEvaluator.VerifyResult {
            val node = immutable.nodes[skillId]
            if (node == null) {
                return ConditionEvaluator.VerifyResult(false, listOf("技能不存在"))
            }
            val conditions = node.levels[1]
            if (conditions == null) {
                return ConditionEvaluator.VerifyResult(false, listOf("未定义 Lv1 条件"))
            }
            return evaluator.verify(conditions, player)
        }

        fun canUpgrade(player: Player, skillId: String): ConditionEvaluator.VerifyResult {
            val playerSkill = skillsById[skillId]
            if (playerSkill == null) {
                return ConditionEvaluator.VerifyResult(false, listOf("未学习"))
            }
            val node = immutable.nodes[skillId]
            if (node == null) {
                return ConditionEvaluator.VerifyResult(false, listOf("技能不存在"))
            }
            if (playerSkill.level >= node.maxLevel) {
                return ConditionEvaluator.VerifyResult(false, listOf("已满级"))
            }
            val conditions = node.levels[playerSkill.level + 1]
            if (conditions == null) {
                return ConditionEvaluator.VerifyResult(false, listOf("未定义 Lv${playerSkill.level + 1} 条件"))
            }
            return evaluator.verify(conditions, player)
        }
    }
}
