package com.gitee.planners.core.skill.condition

data class SkillCastConditionConfig(
    val id: String,
    val props: Map<String, Any>,
    val costTiming: CastCostTiming?
)
