package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.util.replaceInfix
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.sendLang

object PlayerRouteTransferUI : SingletonChoiceUI<ImmutableRoute>("route-transfer.yml") {

    override fun getElements(player: Player): Collection<ImmutableRoute> {
        val playerRouter = player.plannersTemplate.playerRouter
        if (playerRouter == null) {
            return emptyList()
        }
        return playerRouter.getNextRoutes()
    }

    override fun onClick(event: ClickEvent, element: ImmutableRoute) {
        val player = event.clicker
        // 转职条件在提交前校验；不满足时只提示，不写入路线也不扣费。
        if (!element.isConditionSatisfied(player)) {
            player.sendLang("player-transfer-invalid")
            return
        }
        PlayerTemplateAPI.setPlayerRoute(player, element).thenAccept { newRoute ->
            if (newRoute != null) {
                player.sendLang("player-transfer-success", element.getJob().renderName(player))
            }
        }
    }

    override fun onGenerate(player: Player, element: ImmutableRoute, index: Int, slot: Int): ItemStack {
        return (element.icon ?: XMaterial.STONE.parseItem())!!.replaceInfix("\$message") {
            // 条件原文直接展示，便于玩家知道自己差什么。
            val condition = element.condition
            if (condition == null) {
                emptyList<String>()
            } else {
                listOf("&7条件: &f$condition")
            }
        }
    }

}
