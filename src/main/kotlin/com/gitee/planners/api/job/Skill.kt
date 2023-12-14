package com.gitee.planners.api.job

import com.gitee.planners.api.common.Unique

interface Skill : Unique {


    fun getVariables(): Map<String, Variable>

    fun getVariableOrNull(id: String): Variable?

}
