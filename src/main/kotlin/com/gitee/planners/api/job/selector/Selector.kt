package com.gitee.planners.api.job.selector

import com.gitee.planners.api.common.script.kether.SimpleKetherParser


interface Selector {

    fun namespace(): Array<String>

    fun action(): SimpleKetherParser


}
