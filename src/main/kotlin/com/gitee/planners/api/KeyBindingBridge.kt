package com.gitee.planners.api

import com.gitee.planners.core.skill.binding.CombinedAnalyzer
import com.gitee.planners.core.skill.binding.InteractionAction
import org.bukkit.entity.Player

/**
 * 供外部插件（如 FightCore）接入 Planners KeyBinding 系统的桥接 API。
 *
 * 使用方式：在外部插件监听自定义按键事件，将按键码传入 [processKeyAction]。
 */
object KeyBindingBridge {

    /**
     * 将一次按键动作送入 Planners 的 CombinedAnalyzer 进行组合键匹配。
     *
     * @param player 触发按键的玩家
     * @param keyCode 按键标识码（如 mouse.left, keyboard.r），需与 Planners keybinding 的 mapping 字段一致
     */
    fun processKeyAction(player: Player, keyCode: String) {
        CombinedAnalyzer.processAction(player, BridgeKeyAction(keyCode))
    }

    private class BridgeKeyAction(override val code: String) : InteractionAction
}
