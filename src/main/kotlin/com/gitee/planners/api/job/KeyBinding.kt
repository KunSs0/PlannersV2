package com.gitee.planners.api.job

import com.gitee.planners.api.common.registry.Sortable
import com.gitee.planners.api.common.registry.Unique

interface KeyBinding : Unique, Sortable {

    val name: String

}
