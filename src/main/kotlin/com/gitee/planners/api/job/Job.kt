package com.gitee.planners.api.job

import com.gitee.planners.api.common.registry.Unique

interface Job : Unique {

    val name: String

    fun getSkillOrNull(id: String): Skill?

    fun hasSkill(id: String): Boolean

    fun getVariables(): Map<String, Variable>

    fun getVariableOrNull(id: String): Variable?


}
