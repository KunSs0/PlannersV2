package com.gitee.planners.module.script.functions;

import com.gitee.planners.module.script.mythicmobs.MythicObject;
import com.gitee.planners.module.script.GlobalFunctions;

/**
 * MythicMobs 入口函数
 * <p>
 * 迁移自 {@code MythicMobsExtensions.kt}。
 * 链式方法 (spawnMob, isMythicMob) 由 {@link MythicObject} Java 对象直接提供。
 * <pre>{@code
 * // JS: mythic().spawnMob("SkeletonKing", location)
 * // JS: mythic().isMythicMob(entity)
 * }</pre>
 */
public final class MythicMobsFunctions {

    private MythicMobsFunctions() {}

    public static void register() {
        // mythic() — 返回 MythicObject 单例
        GlobalFunctions.register("mythic", args -> MythicObject.INSTANCE);
    }
}
