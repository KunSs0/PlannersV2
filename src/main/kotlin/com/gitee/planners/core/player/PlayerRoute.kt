package com.gitee.planners.core.player

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.common.registry.MutableRegistry
import com.gitee.planners.api.common.registry.Registry
import com.gitee.planners.api.common.registry.defaultRegistry
import com.gitee.planners.api.common.registry.mutableRegistry
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.job.*
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableSkill
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class PlayerRoute(val index: Long, private val routerId: String, private val current: Node, skills: List<PlayerSkill>) :
    Route, Job {

    private val router: Router
        get() = RegistryBuiltin.ROUTER.getOrNull(routerId) ?: error("Could not find router with id $routerId")

    private val route: Route
        get() = router.getRouteOrNull(current.route)!!

    private val job: Job
        @JvmName("job0")
        get() = RegistryBuiltin.JOB.getOrNull(current.route) ?: error("Couldn't find job with id ${current.route}'")

    override val id: String
        get() = routerId

    private val skills = mutableRegistry(skills.map { it.id to it }.toMap())

    override val name: String
        get() = job.name

    override fun getBranches(): List<Route> {
        return route.getBranches()
    }

    fun registerSkill(skill: PlayerSkill) {
        this.skills[skill.id] = skill
    }

    fun getRegistrySkill(): Registry<String, PlayerSkill> {
        return skills
    }

    override fun getJob(): Job {
        return job
    }

    override fun getIcon(): ItemStack? {
        return route.getIcon()
    }

    override fun isInfer(player: Player, options: KetherScriptOptions): Condition.VerifyInfo {
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
