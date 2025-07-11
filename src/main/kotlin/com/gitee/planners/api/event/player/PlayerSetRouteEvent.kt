package com.gitee.planners.api.event.player

import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

/**
 * @Author KunSs
 * @Date 2024/9/17 17:47
 * @version 1.0
 *
 */
class PlayerSetRouteEvent {


    class Pre(val template: PlayerTemplate, val route: ImmutableRoute) : BukkitProxyEvent() {

        val player = template.onlinePlayer

        override val allowCancelled: Boolean
            get() = true

    }

    class Post(val template: PlayerTemplate, val route: PlayerRoute) : BukkitProxyEvent() {

        val player = template.onlinePlayer

        override val allowCancelled: Boolean
            get() = false

    }

}
