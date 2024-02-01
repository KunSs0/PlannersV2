package com.gitee.planners.api.job.selector

import com.gitee.planners.api.common.script.kether.SimpleKetherParser


interface Selector {

    val namespace: Array<String>

    fun select(): SimpleKetherParser

    interface Filterable {

        fun filter(): SimpleKetherParser

    }

}
