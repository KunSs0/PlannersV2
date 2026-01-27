package com.gitee.planners.module.fluxon

import com.gitee.planners.module.fluxon.common.CommonExtensions
import com.gitee.planners.module.fluxon.entity.EntityExtensions
import com.gitee.planners.module.fluxon.sender.SenderExtensions
import com.gitee.planners.module.fluxon.skill.SkillCommands
import com.gitee.planners.module.fluxon.world.LocationExtensions
import org.tabooproject.fluxon.runtime.FluxonRuntime
import taboolib.common.LifeCycle
import taboolib.common.platform.SkipTo
import taboolib.common.platform.function.info

/**
 * Fluxon 模块初始化
 * 在插件启动时注册所有扩展函数
 */
@SkipTo(LifeCycle.ENABLE)
object FluxonLoader {

    /**
     * 初始化 Fluxon 运行时
     * 注册所有扩展函数和 Command
     */
    fun init() {
        // 注册扩展函数
        EntityExtensions.register()
        LocationExtensions.register()
        CommonExtensions.register()
        SkillCommands.register()
        SenderExtensions.register()

        info("[Fluxon] 脚本引擎初始化完成")
    }
}
