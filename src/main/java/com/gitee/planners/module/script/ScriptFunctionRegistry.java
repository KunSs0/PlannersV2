package com.gitee.planners.module.script;

import com.gitee.planners.module.script.functions.*;

import java.util.logging.Logger;

/**
 * 脚本函数注册表
 * <p>
 * 调用全部 Functions 类的 register() 方法。
 * 在 {@link ScriptManager#init()} 中调用。
 */
public final class ScriptFunctionRegistry {

    private static final Logger LOGGER = Logger.getLogger("Script");

    private ScriptFunctionRegistry() {}

    public static void registerAll() {
        // ---- 核心函数 (无外部依赖) ----
        CommonFunctions.register();
        CommandFunctions.register();
        MetadataFunctions.register();
        CooldownFunctions.register();
        HealthFunctions.register();
        StateFunctions.register();
        SkillCommandFunctions.register();
        EffectFunctions.register();
        EntityFunctions.register();
        VelocityFunctions.register();
        PotionFunctions.register();
        SoundFunctions.register();
        ProjectileFunctions.register();
        TargetFinderFunctions.register();
        SkillSystemFunctions.register();
        EconomyFunctions.register();

        // ---- 可选依赖 (缺失时跳过) ----
        tryRegister("MythicMobs", MythicMobsFunctions::register);
        tryRegister("DragonCore", DragonCoreFunctions::register);
        tryRegister("AttributePlus", AttributePlusFunctions::register);
        tryRegister("GermPlugin", GermPluginFunctions::register);
    }

    private static void tryRegister(String name, Runnable registrar) {
        try {
            registrar.run();
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.info("[Script] " + name + " 未检测到，跳过相关函数注册");
        }
    }
}
