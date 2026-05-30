package com.gitee.planners.core.player

import com.gitee.planners.api.Registries
import java.util.concurrent.ConcurrentHashMap
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.api.job.*
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableSkill
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
) : Route, Job {

    val router: Router
        get() = Registries.ROUTER.getOrNull(routerId) ?: error("Could not find router with id $routerId")

    private val route: Route
        get() = router.getRouteOrNull(current.route)!!

    private val job: Job
        @JvmName("job0")
        get() = Registries.JOB.getOrNull(current.route) ?: error("Couldn't find job with id ${current.route}'")

    val algorithmLevel: Algorithm?
        get() = router.algorithmLevel

    override val id: String
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

    override val name: String
        get() = job.name

    override fun getBranches(): List<Route> {
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

    override fun getJob(): Job {
        return job
    }

    override fun getIcon(): ItemStack? {
        return route.getIcon()
    }

    override fun isInfer(player: Player, options: ScriptOptions): Condition.VerifyInfo {
        throw IllegalStateException("Not implemented")
    }

    override fun getVariables(): Map<String, Variable> {
        return job.getVariables()
    }

    override fun getVariableOrNull(id: String): Variable? {
        return job.getVariableOrNull(id)
    }

    override fun hasSkill(id: String): Boolean {
        return this.skills.containsKey(id)
    }

    fun getSkillOrNull(skill: ImmutableSkill): PlayerSkill? {
        return this.getSkillOrNull(skill.id)
    }

    fun getImmutableSkillValues(): List<ImmutableSkill> {
        return (job as ImmutableJob).getImmutableSkillValues()
    }

    fun getImmutableSkill(id: String): ImmutableSkill? {
        return job.getSkillOrNull(id) as? ImmutableSkill
    }

    fun hasImmutableSkill(id: String): Boolean {
        return job.hasSkill(id)
    }

    override fun getSkillOrNull(id: String): PlayerSkill? {
        return this.skills[id]
    }

    /**
     * parent == -1 代表为顶节点
     */
    class Node(val parentId: Long, val route: String)

}
