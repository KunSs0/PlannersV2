package com.gitee.planners.api.job.context

import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.ImmutableSkill
import taboolib.module.kether.ScriptOptions

class ImmutableSkillContext(sender: Target<*>, skill: ImmutableSkill, level: Int) : AbstractSkillContext(sender, skill, level) {
    override val trackId: String
        get() = this.skill.id

    override fun createOptions(block: ScriptOptions.ScriptOptionsBuilder.() -> Unit): KetherScriptOptions {
        return super.createOptions {
            // 注入变量

            block(this)
        }
    }

}
