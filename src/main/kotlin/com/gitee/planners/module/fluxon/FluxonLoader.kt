package com.gitee.planners.module.fluxon

import com.gitee.planners.module.fluxon.command.CommandExtensions
import com.gitee.planners.module.fluxon.common.CommonExtensions
import com.gitee.planners.module.fluxon.cooldown.CooldownExtensions
import com.gitee.planners.module.fluxon.delay.DelayExtensions
import com.gitee.planners.module.fluxon.economy.EconomyExtensions
import com.gitee.planners.module.fluxon.entity.EntityExtensions
import com.gitee.planners.module.fluxon.germplugin.GermPluginExtensions
import com.gitee.planners.module.fluxon.math.MathExtensions
import com.gitee.planners.module.fluxon.metadata.MetadataExtensions
import com.gitee.planners.module.fluxon.mythicmobs.MythicMobsExtensions
import com.gitee.planners.module.fluxon.player.PlayerExtensions
import com.gitee.planners.module.fluxon.profile.ProfileExtensions
import com.gitee.planners.module.fluxon.selector.SelectorExtensions
import com.gitee.planners.module.fluxon.sender.SenderExtensions
import com.gitee.planners.module.fluxon.skill.SkillCommands
import com.gitee.planners.module.fluxon.skillsystem.SkillSystemExtensions
import com.gitee.planners.module.fluxon.velocity.VelocityExtensions
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
        // 注册基础扩展函数
        EntityExtensions.register()
        LocationExtensions.register()
        CommonExtensions.register()
        SkillCommands.register()
        SenderExtensions.register()

        // 高优先级扩展（用于技能配置迁移）
        PlayerExtensions.register()
        MetadataExtensions.register()
        ProfileExtensions.register()
        CooldownExtensions.register()
        CommandExtensions.register()
        DelayExtensions.register()

        // 中优先级扩展
        MathExtensions.register()
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
