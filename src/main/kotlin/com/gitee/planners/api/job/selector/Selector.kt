package com.gitee.planners.api.job.selector


interface Selector {

    val namespace: Array<String>

    interface Filterable

}
