package com.gitee.planners.module.compat.haidercore

import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** 可选插件入口；不施加会与 FightCore 形成环的 Bukkit 加载顺序依赖。 */
object HaiderCoreCompat : WeaponMasteryReader {
    /** 仅在插件启用后访问隔离的强类型 API 实现，缺失时明确拒绝当前请求。 */
    override fun read(playerId: UUID): CompletableFuture<Map<String, Int>> {
        if (!Bukkit.getPluginManager().isPluginEnabled("HaiderCore")) {
            return CompletableFuture.failedFuture(IllegalStateException("武器熟练度条件需要已启用的 HaiderCore"))
        }
        try {
            return HaiderMasteryReader.read(playerId)
        } catch (error: LinkageError) {
            // API 缺失或二进制不匹配必须结束请求，不能绕过门槛或遗留学习锁。
            return CompletableFuture.failedFuture(IllegalStateException("HaiderCore 熟练度 API 缺失或不匹配", error))
        }
    }
}
