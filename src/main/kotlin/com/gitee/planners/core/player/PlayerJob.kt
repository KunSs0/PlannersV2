package com.gitee.planners.core.player

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.Variable
import com.gitee.planners.core.config.ImmutableSkill

class PlayerJob(val bindingId: Long, val jobId: String) : Job {

    private val instance: Job
        get() = RegistryBuiltin.JOB.getOrNull(jobId) ?: error("Couldn't find job with id $jobId'")

    private val skills = mutableMapOf<String, PlayerSkill>()

    override val id: String
        get() = instance.id

    override val name: String
        get() = instance.name

    override fun getVariables(): Map<String, Variable> {
        return instance.getVariables()
    }

    override fun getVariableOrNull(id: String): Variable? {
        return instance.getVariableOrNull(id)
    }

    override fun hasSkill(id: String): Boolean {
        return true
    }

    fun getSkillOrNull(skill: ImmutableSkill): Skill? {
        return this.getSkillOrNull(skill.id)
    }

    override fun getSkillOrNull(id: String): Skill? {
        if (!instance.hasSkill(id)) {
            // 如果没有学习 则添加一个新的（这时候并未在数据库内添加）
            val immutable = instance.getSkillOrNull(id)!! as ImmutableSkill
            this.skills[id] = PlayerSkill(-1, id, immutable.startedLevel)
        }

        return this.skills[id]!!
    }

}
