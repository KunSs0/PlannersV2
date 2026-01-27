package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.util.replaceInfix
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.sendLang

object PlayerRouteTransferUI : SingletonChoiceUI<ImmutableRoute>("route-transfer.yml") {

    override fun getElements(player: Player): Collection<ImmutableRoute> {
        return player.plannersTemplate.route!!.getBranches().map { it as ImmutableRoute }
    }

    override fun onClick(event: ClickEvent, element: ImmutableRoute) {
        val player = event.clicker
        val verify = element.condition.verify(FluxonScriptOptions.common(player))
        // 如果校验不通过
        if (verify.isInvalid) {
            player.sendLang("player-transfer-invalid")
            return
        }
        val template = player.plannersTemplate
        PlayerTemplateAPI.OPERATOR.createPlayerRoute(template,element).thenAccept {
            template.route = it
            player.sendLang("player-transfer-success", element.getJob().name)
        }
    }

    override fun onGenerate(player: Player, element: ImmutableRoute, index: Int, slot: Int): ItemStack {
        return (element.icon ?: XMaterial.STONE.parseItem())!!.replaceInfix("\$message") {
            element.condition.getMessage(FluxonScriptOptions.common(player))
        }
    }

}
