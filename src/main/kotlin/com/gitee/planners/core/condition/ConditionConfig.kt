package com.gitee.planners.core.condition

data class ConditionConfig(
    val key: String,
    val exper: String,
    val props: Map<String, Any>,
    val hint: String,
    val consume: String?
)
