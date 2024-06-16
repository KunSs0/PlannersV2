package com.gitee.planners.api.job

import com.gitee.planners.api.common.Sortable
import com.gitee.planners.api.common.Unique

interface KeyBinding : Unique, Sortable {

    val name: String

}
