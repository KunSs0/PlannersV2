package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.event.player.PlayerSetRouteEvent
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.core.attribute.AttributeProxy
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent

object AttributeProvider {

    @SubscribeEvent
    fun e(e: PlayerProfileLoadedEvent) {
        AttributeProxy.sync(e.player)
    }

    @SubscribeEvent
    fun e(e: PlayerSkillEvent.LevelChange) {
        AttributeProxy.sync(e.player)
    }

    @SubscribeEvent
    fun e(e: PlayerSetRouteEvent.Post) {
        AttributeProxy.sync(e.player)
    }
}
