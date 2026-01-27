package com.gitee.planners.module.fluxon

import taboolib.common.LifeCycle
import taboolib.common.platform.SkipTo
import taboolib.common.platform.function.info

/**
 * Fluxon 模块初始化
 * 所有扩展函数通过 @Awake(LifeCycle.LOAD) 自动注册
 */
@SkipTo(LifeCycle.ENABLE)
object FluxonLoader {

    /**
     * 初始化 Fluxon 运行时
     */
    fun init() {
        // 初始化事件注册表
        FluxonEventRegistry.init()

        info("[Fluxon] 脚本引擎初始化完成")
    }
}
