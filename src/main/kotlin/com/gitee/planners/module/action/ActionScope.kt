package com.gitee.planners.module.action

/**
 * 动作、条件、目标器与触发器允许出现的上下文。
 *
 * Planners 只有技能一种载体，因此作用域按「依赖什么上下文」划分：
 * 通用项不依赖技能树与目标实体，技能项则要求存在技能上下文。
 */
enum class ActionScope {

    /**
     * 通用：不依赖技能树与目标实体。
     */
    GENERAL,

    /**
     * 技能：要求存在技能上下文与目标实体。
     */
    SKILL,
}
