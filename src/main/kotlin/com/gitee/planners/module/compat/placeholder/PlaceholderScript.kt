package com.gitee.planners.module.compat.placeholder

import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import org.bukkit.entity.Player

/**
 * PlaceholderScript 占位符脚本
 */
object PlaceholderScript {

    fun parse(player: Player, args: String): String {
        val script = SingletonFluxonScript(args)
        val options = FluxonScriptOptions.common(player)
        return script.eval(options)?.toString() ?: ""
    }

}
