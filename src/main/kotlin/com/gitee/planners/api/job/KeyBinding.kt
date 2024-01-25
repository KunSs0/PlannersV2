package com.gitee.planners.api.job

import com.gitee.planners.api.common.registry.Unique

interface KeyBinding : Unique {

    val name: String

}
