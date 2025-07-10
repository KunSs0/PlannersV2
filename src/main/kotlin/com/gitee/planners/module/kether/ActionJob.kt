package com.gitee.planners.module.kether

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.MultipleKetherParser

@CombinationKetherParser.Used
object ActionJob : MultipleKetherParser("job") {

    val _id = ActionProfile.processNow("id") {
        it.plannersTemplate.route?.getJob()?.id
    }

    val name = ActionProfile.processNow("name") {
        it.plannersTemplate.route?.getJob()?.name
    }

}