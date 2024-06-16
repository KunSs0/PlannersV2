package com.gitee.planners.api.job

import com.gitee.planners.api.common.Unique

interface Job : Unique, VariableProvider {

    val name: String

    fun getSkillOrNull(id: String): Skill?

    fun hasSkill(id: String): Boolean

}
