package com.gitee.planners.module.compat

import com.gitee.planners.api.event.entity.EntityModelApplyEvent
import com.ticxo.modelengine.api.ModelEngineAPI
import taboolib.common.platform.event.SubscribeEvent

object ModelEngineHooked {

    private val hooked by lazy {
        isPluginHooked("com.ticxo.modelengine.ModelEngine")
    }

    @SubscribeEvent
    fun e(e: EntityModelApplyEvent) {
        if (this.hooked) {
            val modeledEntity = ModelEngineAPI.createModeledEntity(e.entity)
            val activeModel = ModelEngineAPI.createActiveModel(e.model)
            modeledEntity.addActiveModel(activeModel)

        }
    }

}
