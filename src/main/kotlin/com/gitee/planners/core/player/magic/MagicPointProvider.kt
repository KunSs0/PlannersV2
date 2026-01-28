package com.gitee.planners.core.player.magic

import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

interface MagicPointProvider {

    companion object {

        /**
         * 魔法点数提供器
         * 当第三方调用的时候一定要进行注销默认提供的 provider
         * (INSTANCE as? DefaultMagicPointProvider)?.close
         */
        lateinit var INSTANCE: MagicPointProvider

        @JvmStatic
        @Awake(LifeCycle.ENABLE)
        fun init() {
            // 注册默认提供器
            if (!::INSTANCE.isInitialized) {
                INSTANCE = DefaultMagicPointProvider()
            }
        }

        val PlayerTemplate.magicPointInUpperLimit: Int
            get() = INSTANCE.getPointInUpperLimit(this.onlinePlayer)

        var PlayerTemplate.magicPoint: Int
            get() = INSTANCE.getPoint(this.onlinePlayer)
            set(value) {
                INSTANCE.setPoint(this.onlinePlayer, value)
            }

    }

    /**
     * Get the magic point.
     * @return the magic point
     */
    fun getPoint(player: Player): Int

    /**
     * Set the magic point.
     * @param magicPoint the magic point
     */
    fun setPoint(player: Player,magicPoint: Int)

    /**
     * Get the magic point in the lower limit.
     * @return the magic point in the lower limit
     */
    fun getPointInUpperLimit(player: Player): Int

}
