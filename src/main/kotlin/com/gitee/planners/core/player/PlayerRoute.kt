package com.gitee.planners.core.player

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Variable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submitAsync

class PlayerRoute(
    val bindingId: Long,
    private val routerId: String,
    private val current: Node,
    skills: List<PlayerSkill>,
    /** 当前可用技能点（初始值，来自数据库） */
    initialSPCurrent: Int = 0,
    /** 累计已消耗技能点（初始值，来自数据库） */
    initialSPUsed: Int = 0
) {

    val router: ImmutableRouter
        get() = Registries.ROUTER.getOrNull(routerId) ?: error("Could not find router with id $routerId")

    private val route: ImmutableRoute
        get() = router.getRouteOrNull(current.route)!!

    private val job: ImmutableJob
        @JvmName("job0")
        get() = Registries.JOB.getOrNull(current.route) ?: error("Couldn't find job with id ${current.route}'")

    val algorithmLevel: Algorithm?
        get() = router.algorithmLevel

    val id: String
        get() = routerId

    private val skills = mutableMapOf(*skills.map { it.id to it }.toTypedArray())

    /** 当前可用技能点 */
    var skillPointsCurrent: Int = initialSPCurrent
        private set

    /** 累计已消耗技能点 */
    var skillPointsUsed: Int = initialSPUsed
        private set

    /** 增加技能点（升级时） */
    fun addSkillPoints(amount: Int) {
        skillPointsCurrent = maxOf(0, skillPointsCurrent + amount)
        submitAsync { Database.INSTANCE.updateSkillPoints(this@PlayerRoute) }
    }

    /** 消耗技能点（学技能时），返回是否成功 */
    fun takeSkillPoints(amount: Int): Boolean {
        if (skillPointsCurrent < amount) return false
        skillPointsCurrent -= amount
        skillPointsUsed += amount
        submitAsync { Database.INSTANCE.updateSkillPoints(this@PlayerRoute) }
        return true
    }

    val name: String
        get() = job.name

    fun getBranches(): List<ImmutableRoute> {
        return route.getBranches()
    }

    private val equippedByPageSlot = ConcurrentHashMap<String, PlayerSkill>()

    init {
        skills.forEach { skill ->
            if (skill.equipped && skill.backpackPage != null && skill.backpackSlot != null) {
                equippedByPageSlot["${skill.backpackPage}:${skill.backpackSlot}"] = skill
            }
        }
    }

    fun registerSkill(skill: PlayerSkill) {
        this.skills[skill.id] = skill
        if (skill.equipped && skill.backpackPage != null && skill.backpackSlot != null) {
            equippedByPageSlot["${skill.backpackPage}:${skill.backpackSlot}"] = skill
        }
    }

    fun getEquippedSkill(page: String, slot: String): PlayerSkill? {
        return equippedByPageSlot["$page:$slot"]
    }

    fun updateEquippedIndex(skill: PlayerSkill) {
        equippedByPageSlot.entries.removeIf { it.value == skill }
        if (skill.equipped && skill.backpackPage != null && skill.backpackSlot != null) {
            equippedByPageSlot["${skill.backpackPage}:${skill.backpackSlot}"] = skill
        }
    }

    fun getRegisteredSkill(): Map<String, PlayerSkill> {
        return skills
    }

    fun getJob(): ImmutableJob {
        return job
    }

    fun getIcon(): ItemStack? {
        return route.getIcon()
    }

    fun getVariables(): Map<String, Variable> {
        return job.getVariables()
    }

    fun getVariableOrNull(id: String): Variable? {
        return job.getVariableOrNull(id)
    }

    fun hasSkill(id: String): Boolean {
        return this.skills.containsKey(id)
    }

    fun getSkillOrNull(skill: ImmutableSkill): PlayerSkill? {
        return this.getSkillOrNull(skill.id)
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

    fun getSkillOrNull(id: String): PlayerSkill? {
        return this.skills[id]
    }

    // ---- SkillTree ----

    /** 解析当前路线绑定的技能树 ID */
    private fun resolveSkillTreeId(): String? {
        val router = Registries.ROUTER.getOrNull(routerId) ?: return null
        val route = router.getRouteOrNull(current.route) ?: return null
        return route.skillTree
    }

    /** 当前激活的技能树（每次实时查询 Registries） */
    val skillTree: SkillTree?
        get() {
            val treeId = resolveSkillTreeId() ?: return null
            val immutable = Registries.SKILL_TREE.getOrNull(treeId) ?: return null
            return SkillTree(immutable)
        }

    /**
     * 技能树内部类。
     * 封装技能的学习与升级逻辑。
     */
    inner class SkillTree(
        val immutable: ImmutableSkillTree
    ) {
        private val evaluator = ConditionEvaluator()

        val treeId: String
            get() = immutable.id

        /** 获取已学技能等级（未学返回 0） */
        fun getLevel(skillId: String): Int {
            return skills[skillId]?.level ?: 0
        }

        /** 获取所有已学技能 */
        fun getLearnedSkills(): Map<String, PlayerSkill> {
            return skills.filterKeys { immutable.nodes.containsKey(it) }
        }

        /**
         * 首次学习技能。
         * 校验 Lv1 条件并消耗，最后创建 PlayerSkill。
         */
        fun learn(player: Player, skillId: String): CompletableFuture<Void> {
            // 1. 确认未学（level>0 才视为已学习，level=0 只是已注册）
            val existing = skills[skillId]
            if (existing != null && existing.level > 0) {
                throw IllegalStateException("技能 $skillId 已学习")
            }
            // 2. 确认节点存在
            val node = immutable.nodes[skillId]
                ?: throw IllegalArgumentException("技能树 '${immutable.id}' 中不存在技能 '$skillId'")
            val conditions = node.levels[1]
                ?: throw IllegalArgumentException("技能 '$skillId' 未定义 Lv1 条件")

            // 3. 校验
            val result = evaluator.verify(conditions, player)
            if (!result.passed) {
                throw IllegalStateException("不满足学习条件: ${result.hints.joinToString(", ")}")
            }

            // 4. 消耗
            evaluator.consume(conditions, player)

            // 5. 使用已有或创建 PlayerSkill
            val template = player.plannersTemplate
            if (existing != null) {
                PlayerTemplateAPI.setSkillLevel(template, existing, 1)
                return CompletableFuture.completedFuture(null)
            }

            val immutable = getImmutableSkill(skillId)
                ?: throw IllegalArgumentException("ImmutableSkill '$skillId' 不存在")

            return Database.INSTANCE.createPlayerSkill(template, immutable).thenApply { ps ->
                registerSkill(ps)
                PlayerTemplateAPI.setSkillLevel(template, ps, 1)
                null
            }
        }

        /**
         * 升级已学技能。
         * 校验目标等级条件并消耗。
         */
        fun upgrade(player: Player, skillId: String): CompletableFuture<Void> {
            // 1. 确认已学
            val ps = skills[skillId]
                ?: throw IllegalStateException("技能 $skillId 未学习")
            // 2. 确认可升级
            val node = immutable.nodes[skillId]
                ?: throw IllegalArgumentException("技能树中不存在技能 '$skillId'")
            if (ps.level >= node.maxLevel) {
                throw IllegalStateException("技能 $skillId 已满级 (${ps.level}/${node.maxLevel})")
            }
            val targetLevel = ps.level + 1
            val conditions = node.levels[targetLevel]
                ?: throw IllegalArgumentException("技能 '$skillId' 未定义 Lv$targetLevel 条件")

            // 3. 校验
            val result = evaluator.verify(conditions, player)
            if (!result.passed) {
                throw IllegalStateException("不满足升级条件: ${result.hints.joinToString(", ")}")
            }

            // 4. 消耗
            evaluator.consume(conditions, player)

            // 5. 升级
            val template = player.plannersTemplate
            PlayerTemplateAPI.setSkillLevel(template, ps, targetLevel)

            return CompletableFuture.completedFuture(null)
        }

        /** 判断技能是否可学（所有前置条件满足） */
        fun canLearn(player: Player, skillId: String): ConditionEvaluator.VerifyResult {
            val node = immutable.nodes[skillId] ?: return ConditionEvaluator.VerifyResult(false, listOf("技能不存在"))
            val conditions = node.levels[1] ?: return ConditionEvaluator.VerifyResult(false, listOf("未定义 Lv1 条件"))
            return evaluator.verify(conditions, player)
        }

        /** 判断技能是否可升级 */
        fun canUpgrade(player: Player, skillId: String): ConditionEvaluator.VerifyResult {
            val ps = skills[skillId] ?: return ConditionEvaluator.VerifyResult(false, listOf("未学习"))
            val node = immutable.nodes[skillId] ?: return ConditionEvaluator.VerifyResult(false, listOf("技能不存在"))
            if (ps.level >= node.maxLevel) {
                return ConditionEvaluator.VerifyResult(false, listOf("已满级"))
            }
            val conditions = node.levels[ps.level + 1]
                ?: return ConditionEvaluator.VerifyResult(false, listOf("未定义 Lv${ps.level + 1} 条件"))
            return evaluator.verify(conditions, player)
        }
    }

    /**
     * parent == -1 代表为顶节点
     */
    class Node(val parentId: Long, val route: String)

}
