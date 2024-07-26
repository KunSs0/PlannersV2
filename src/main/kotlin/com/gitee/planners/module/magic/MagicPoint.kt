package com.gitee.planners.module.magic

import com.gitee.planners.core.player.PlayerTemplate
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object MagicPoint {

    /**
     * 魔法点数提供器
     * 当第三方调用的时候一定要进行注销默认提供的 provider
     * (INSTANCE as? DefaultMagicPointProvider)?.close
     */
    lateinit var INSTANCE: MagicPointProvider

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
