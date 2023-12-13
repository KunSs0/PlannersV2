package com.gitee.planners.api.job.selector

import taboolib.library.kether.QuestActionParser


interface Selector {

    fun namespace(): Array<String>

    fun action(): QuestActionParser


}
