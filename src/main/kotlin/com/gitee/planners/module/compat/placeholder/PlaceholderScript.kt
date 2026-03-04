package com.gitee.planners.module.compat.placeholder

import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import org.bukkit.entity.Player

/**
 * PlaceholderScript 占位符脚本
 */
object PlaceholderScript {

    fun parse(player: Player, args: String): String {
        val script = SingletonScript(args)
        val options = ScriptOptions.common(player)
        return script.eval(options)?.toString() ?: ""
    }

}
