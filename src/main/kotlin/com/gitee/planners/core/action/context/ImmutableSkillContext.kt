package com.gitee.planners.core.action.context

import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.ImmutableSkill
import taboolib.module.kether.ScriptOptions

open class ImmutableSkillContext(sender: Target<*>, skill: ImmutableSkill, level: Int) : AbstractSkillContext(sender, skill, level) {
    override val trackId: String
        get() = this.skill.id

}
