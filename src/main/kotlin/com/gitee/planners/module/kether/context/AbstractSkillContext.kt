package com.gitee.planners.module.kether.context

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.ImmutableSkill

abstract class AbstractSkillContext(sender: Target<*>, val skill: ImmutableSkill, var level: Int) : AbstractComplexScriptContext(sender, skill) {

    val variables = skill.getVariables().map {
        it.key to lazy {
            // 注入基础配置项 但不注入变量
            it.value.run(super.optionsBuilder { })
        }
    }.toMap()

}
