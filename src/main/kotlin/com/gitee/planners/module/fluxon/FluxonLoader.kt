package com.gitee.planners.module.fluxon

import com.gitee.planners.module.fluxon.delay.DelayExtensions
import com.gitee.planners.module.fluxon.economy.EconomyExtensions
import com.gitee.planners.module.fluxon.germplugin.GermPluginExtensions
import com.gitee.planners.module.fluxon.metadata.MetadataExtensions
import com.gitee.planners.module.fluxon.mythicmobs.MythicMobsExtensions
import com.gitee.planners.module.fluxon.profile.ProfileExtensions
import com.gitee.planners.module.fluxon.selector.SelectorExtensions
import com.gitee.planners.module.fluxon.skill.SkillCommands
import com.gitee.planners.module.fluxon.skillsystem.SkillSystemExtensions
import com.gitee.planners.module.fluxon.velocity.VelocityExtensions
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
        // 注册基础扩展函数
        SkillCommands.register()

        // 中优先级扩展
        MetadataExtensions.register()
        ProfileExtensions.register()
        DelayExtensions.register()
        VelocityExtensions.register()
        SelectorExtensions.register()
        SkillSystemExtensions.register()
        EconomyExtensions.register()

        // 低优先级扩展（第三方集成）
        MythicMobsExtensions.register()
        GermPluginExtensions.register()

        // 初始化事件注册表
        FluxonEventRegistry.init()

        info("[Fluxon] 脚本引擎初始化完成")
    }
}
