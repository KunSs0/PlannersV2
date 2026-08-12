package com.gitee.planners.module.compat.placeholder

import org.bukkit.entity.Player
import taboolib.platform.compat.PlaceholderExpansion

/**
 * Planners 的 PlaceholderAPI 扩展入口。
 *
 * 该扩展只接受由 [PlaceholderLiteral] 定义的字面量查询协议，禁止将占位符参数作为脚本执行。
 */
object PlaceholderHooked : PlaceholderExpansion {

    override val identifier: String
        get() = "planners"

    /**
     * 解析 PlaceholderAPI 发起的占位符请求。
     *
     * @param player PlaceholderAPI 提供的玩家上下文；无玩家上下文时无法读取玩家档案。
     * @param args 移除扩展标识后的占位符参数。
     * @return 成功解析后的文本；上下文或参数无效时返回空字符串。
     */
    override fun onPlaceholderRequest(player: Player?, args: String): String {
        if (player == null) {
            return ""
        }

        // 仅将文本交给字面量路由器，避免 PAPI 查询取得脚本执行能力。
        return PlaceholderLiteral.parse(player, args)
    }

}
