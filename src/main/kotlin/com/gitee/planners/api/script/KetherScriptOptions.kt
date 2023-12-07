package com.gitee.planners.api.script

import org.bukkit.entity.Player
import taboolib.module.kether.ScriptOptions

interface KetherScriptOptions {

    fun build(): ScriptOptions.ScriptOptionsBuilder

    companion object {

        fun sender(player: Player, options: KetherScriptOptions): KetherScriptOptions {
            return object : KetherScriptOptions {
                override fun build(): ScriptOptions.ScriptOptionsBuilder {
                    return options.build().sender(player)
                }
            }
        }

    }

}
