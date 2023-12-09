package com.gitee.planners.core.ui

import com.gitee.planners.api.ProfileAPI
import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.util.replaceInfix
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

object RouteTransferUI : AutomationBaseUI("route-transfer.yml") {

    @BaseUI.Option("__option__.rows")
    val rows = 6

    @BaseUI.Option("__option__.title")
    val title = "Chest"

    @BaseUI.Option("__option__.slots")
    val slots = simpleConfigNodeTo<List<Any>, List<Int>> {
        map { it.cint }
    }

    @BaseUI.Option("*")
    val decorateIcon = decorateIcon()


    override fun display(player: Player): BaseUI.Display {
        val profile = player.plannersProfile
        return BaseUI.linked<ImmutableRoute>(title) {
            rows(this@RouteTransferUI.rows)
            slots(this@RouteTransferUI.slots.get())
            elements { profile.route!!.getBranches().map { it as ImmutableRoute } }
            // 注入装饰品
            injectDecorateIcon(RouterSelectUI.decorateIcon.get())

            onGenerate { player, element, index, slot ->
                (element.icon ?: XMaterial.STONE.parseItem())!!.replaceInfix("\$message") {
                    element.condition.getMessage(KetherScriptOptions.common(player))
                }
            }

            onClick { event, element ->
                val verify = element.condition.verify(KetherScriptOptions.common(player))
                // 如果校验不通过
                if (verify.isInvalid) {
                    player.sendLang("player-transfer-invalid")
                    return@onClick
                }
                ProfileAPI.modified(player) {
                    this.transferRoute(element).thenAccept {
                        player.sendLang("player-transfer-success",element.getJob().name)
                    }
                }
            }
        }
    }
}
