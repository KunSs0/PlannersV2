package com.gitee.planners.api.common.script

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import org.bukkit.entity.Player
import taboolib.module.kether.ScriptOptions

interface KetherScriptOptions {

    fun build(): ScriptOptions.ScriptOptionsBuilder

    companion object {

        /**
         * 公共命名空间
         */
        fun common(player: Player) = common(TargetBukkitEntity(player))

        fun common(sender: Target<*>) = create {
            if (sender is TargetBukkitEntity && sender.getInstance() is Player) {
                sender(sender.getInstance())
            }
            namespace(listOf(KetherHelper.NAMESPACE_COMMON))
        }

        fun create(builder: ScriptOptions.ScriptOptionsBuilder.() -> Unit): KetherScriptOptions {
            return generic {
                ScriptOptions.ScriptOptionsBuilder().also {
                    it.sandbox(true)
                    builder(it)
                }
            }
        }

        fun generic(func: () -> ScriptOptions.ScriptOptionsBuilder): KetherScriptOptions {
            return object : KetherScriptOptions {
                override fun build(): ScriptOptions.ScriptOptionsBuilder {
                    return func()
                }
            }
        }

        fun sender(player: Player, options: KetherScriptOptions): KetherScriptOptions {
            return generic { options.build().sender(player) }
        }

    }

}
