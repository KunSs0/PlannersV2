package com.gitee.planners.api.job

import com.gitee.planners.api.common.Sortable
import com.gitee.planners.api.common.Unique
import com.gitee.planners.module.binding.Combined

interface KeyBinding : Unique, Sortable {

    val name: String

}
