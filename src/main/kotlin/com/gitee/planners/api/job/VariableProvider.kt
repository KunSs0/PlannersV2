package com.gitee.planners.api.job

interface VariableProvider {

    fun getVariables(): Map<String, Variable>

    fun getVariableOrNull(id: String): Variable?

    fun getVariable(id: String): Variable {
        return getVariableOrNull(id) ?: throw IllegalArgumentException("Variable with id '$id' not found")
    }

}
