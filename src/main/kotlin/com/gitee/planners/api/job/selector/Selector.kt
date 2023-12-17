package com.gitee.planners.api.job.selector

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import taboolib.library.kether.QuestActionParser


interface Selector {

    fun namespace(): Array<String>

    fun action(): QuestActionParser


}
