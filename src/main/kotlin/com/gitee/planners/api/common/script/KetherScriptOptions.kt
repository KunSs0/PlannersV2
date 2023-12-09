package com.gitee.planners.api.common.script

import org.bukkit.entity.Player
import taboolib.module.kether.ScriptOptions

interface KetherScriptOptions {

    fun build(): ScriptOptions.ScriptOptionsBuilder

    companion object {

        fun generic(player: Player): KetherScriptOptions {
            return create {
                sender(player)
            }
        }

        /**
         * 公共命名空间
         */
        fun common(player: Player) = create {
            sender(player)
            namespace(listOf("planners-common"))
        }

        fun create(builder: ScriptOptions.ScriptOptionsBuilder.() -> Unit): KetherScriptOptions {
            return object : KetherScriptOptions {
                override fun build(): ScriptOptions.ScriptOptionsBuilder {
                    return ScriptOptions.ScriptOptionsBuilder().also {
                        it.sandbox(true)
                        builder(it)
                    }
                }
            }
        }

        fun sender(player: Player, options: KetherScriptOptions): KetherScriptOptions {
            return object : KetherScriptOptions {
                override fun build(): ScriptOptions.ScriptOptionsBuilder {
                    return options.build().sender(player)
                }
            }
        }

    }

}
