package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.job.selector.Selector

abstract class AbstractSelector(vararg name: String) : Selector {

    override val namespace = arrayOf(*name)

}
