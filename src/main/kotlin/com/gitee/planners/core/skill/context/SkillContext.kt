package com.gitee.planners.core.skill.context

import com.gitee.planners.api.context.Context
import com.gitee.planners.api.directing.DirectingResult
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

    /**
     * 本次技能确认后的指向性结果。
     *
     * 普通技能或尚未确认的指向性技能保持 null。具体结果类型由 directing provider 定义，
     * 不复用脚本系统既有的 target 语义。
     */
    var directing: DirectingResult? = null

    companion object {
        val EMPTY = SkillContext(null, null, 0)
    }
}
