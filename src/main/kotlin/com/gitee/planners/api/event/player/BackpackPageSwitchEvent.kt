package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

open class BackpackPageSwitchEvent(
    val template: PlayerTemplate,
    val fromPage: String?,
    val toPage: String
) : BukkitProxyEvent() {

    val player = template.onlinePlayer

    class Pre(template: PlayerTemplate, fromPage: String?, toPage: String) :
        BackpackPageSwitchEvent(template, fromPage, toPage)

    class Post(template: PlayerTemplate, fromPage: String?, toPage: String) :
        BackpackPageSwitchEvent(template, fromPage, toPage)
}
