package com.gitee.planners.api.common.script.kether

import taboolib.library.kether.QuestActionParser

interface CombinationKetherParser {

    val id: Array<String>

    val namespace: String

    fun run(): QuestActionParser

    annotation class Used

    annotation class Ignore
}
