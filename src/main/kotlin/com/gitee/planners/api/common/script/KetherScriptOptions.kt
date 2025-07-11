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
         * 通用的Kether选项
         *
         * @param player 玩家
         *
         * @return Kether选项
         */
        fun common(player: Player) = common(TargetBukkitEntity(player))

        /**
         * 通用的Kether选项
         *
         * @param sender 发送者
         *
         * @return Kether选项
         */
        fun common(sender: Target<*>) = create {
            if (sender is TargetBukkitEntity && sender.instance is Player) {
                sender(sender.instance)
            }
            namespace(listOf(KetherHelper.NAMESPACE_COMMON))
        }

        /**
         * 创建Kether选项
         *
         * @param builder 选项构建器
         *
         * @return Kether选项
         */
        fun create(builder: ScriptOptions.ScriptOptionsBuilder.() -> Unit): KetherScriptOptions {
            return generic {
                ScriptOptions.ScriptOptionsBuilder().also {
                    it.sandbox(true)
                    builder(it)
                }
            }
        }

        /**
         * 创建Kether选项
         *
         * @param func 选项构建器
         *
         * @return Kether选项
         */
        fun generic(func: () -> ScriptOptions.ScriptOptionsBuilder): KetherScriptOptions {
            return object : KetherScriptOptions {
                override fun build(): ScriptOptions.ScriptOptionsBuilder {
                    return func()
                }
            }
        }

        /**
         * 设置发送者
         *
         * @param player 玩家
         * @param options 选项
         *
         * @return Kether选项
         */
        fun sender(player: Player, options: KetherScriptOptions): KetherScriptOptions {
            return generic { options.build().sender(player) }
        }

    }

}
