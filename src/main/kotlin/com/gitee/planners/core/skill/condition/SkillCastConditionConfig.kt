package com.gitee.planners.core.skill.condition

/**
 * 一条已编译的技能释放条件。
 *
 * 条件只声明标识与参数；成本提交时机统一为「所有条件校验通过后一次性提交」，
 * 不再由单条配置决定。
 *
 * @property id 条件标识，对应注册表里的实现。
 * @property props 条件参数，加载期已编译，释放期完成求值。
 */
data class SkillCastConditionConfig(
    val id: String,
    val props: Map<String, Any>
)
