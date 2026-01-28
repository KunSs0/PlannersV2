package com.gitee.planners.core.skill.context

import com.gitee.planners.api.context.Context
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.ImmutableSkill

/**
 * 技能执行上下文
 */
class SkillContext(
    override val sender: ProxyTarget<*>?,
    val skill: ImmutableSkill?,
    var level: Int = 0
) : Context {

    override var origin: ProxyTarget<*>? = sender

    companion object {
        val EMPTY = SkillContext(null, null, 0)
    }
}
