package com.gitee.planners.module.kether.context

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.ImmutableSkill

open class ImmutableSkillContext(sender: Target<*>, skill: ImmutableSkill, level: Int) : AbstractSkillContext(sender, skill, level) {
    override val trackId: String
        get() = this.skill.id

}
