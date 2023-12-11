package com.gitee.planners.core.ui

import com.gitee.planners.api.ProfileAPI
import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.util.replaceInfix
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.sendLang

object RouteTransferUI : SingletonChoiceUI<ImmutableRoute>("route-transfer.yml") {

    override fun getElements(player: Player): List<ImmutableRoute> {
        return player.plannersProfile.route!!.getBranches().map { it as ImmutableRoute }
    }

    override fun onClick(event: ClickEvent, element: ImmutableRoute) {
        val player = event.clicker
        val verify = element.condition.verify(KetherScriptOptions.common(player))
        // 如果校验不通过
        if (verify.isInvalid) {
            player.sendLang("player-transfer-invalid")
            return
        }
        ProfileAPI.modified(player) {
            this.transferRoute(element).thenAccept {
                player.sendLang("player-transfer-success", element.getJob().name)
            }
        }
    }

    override fun onGenerate(player: Player, element: ImmutableRoute, index: Int, slot: Int): ItemStack {
        return (element.icon ?: XMaterial.STONE.parseItem())!!.replaceInfix("\$message") {
            element.condition.getMessage(KetherScriptOptions.common(player))
        }
    }

}
