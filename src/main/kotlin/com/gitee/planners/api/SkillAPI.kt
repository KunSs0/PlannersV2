package com.gitee.planners.api

import com.gitee.planners.api.common.Registry
import com.gitee.planners.core.config.ImmutableSkill
import taboolib.module.configuration.Configuration

object SkillAPI : Registry.AbstractBuiltin<ImmutableSkill>() {

    override fun invokeInstance(config: Configuration): ImmutableSkill {
        return ImmutableSkill(config)
    }

}
