package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.damage.DamageCause
import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.api.job.target.ProxyTarget
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.skills.placeholders.Placeholder
import io.lumine.xikage.mythicmobs.skills.placeholders.PlaceholderMeta
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.server.PluginEnableEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import java.util.function.BiFunction

/**
 * 把 Planners 的实体元数据暴露为 MythicMobs 占位符，并在自定义伤害后同步威胁。
 *
 * MythicMobs 4 与 5 的 API 分离，两套实现按主版本号分派。
 */
object MythicMobDamageHandler {

    @Awake(LifeCycle.ENABLE)
    fun init() {
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            registerPlaceholder()
        }
    }

    @SubscribeEvent
    fun onPluginEnable(event: PluginEnableEvent) {
        if (event.plugin.name != "MythicMobs") {
            return
        }
        registerPlaceholder()
    }

    private fun registerPlaceholder() {
        when (MythicMobsLoader.version.getOrNull(0)) {
            4 -> registerMythicMobs4Placeholder()
            5 -> registerMythicMobs5Placeholder()
            else -> info(
                "MythicMobDamageHandler: Unsupported MythicMobs version ${MythicMobsLoader.version.joinToString(".")}"
            )
        }
    }

    private fun registerMythicMobs4Placeholder() {
        val placeholder = Placeholder.meta(object : BiFunction<PlaceholderMeta, String, String> {

            override fun apply(metadata: PlaceholderMeta, args: String): String {
                return resolveMetadata(metadata.caster.entity, args)
            }
        })
        MythicMobs.inst().placeholderManager.register("caster.pl.metadata", placeholder)
    }

    private fun registerMythicMobs5Placeholder() {
        val placeholder = io.lumine.mythic.core.skills.placeholders.Placeholder.meta(
            object : BiFunction<io.lumine.mythic.core.skills.placeholders.PlaceholderMeta, String, String> {

                override fun apply(
                    metadata: io.lumine.mythic.core.skills.placeholders.PlaceholderMeta,
                    args: String
                ): String {
                    return resolveMetadata(metadata.caster.entity, args)
                }
            }
        )
        MythicBukkit.inst().placeholderManager.register("caster.pl.metadata", placeholder)
    }

    /**
     * 读取实体上的 Planners 元数据。
     *
     * @param source MythicMobs 提供的施法者实体（两版 API 的 AbstractEntity）。
     * @param key 元数据键。
     * @return 元数据文本；键不存在或目标不支持元数据时返回 `null` 字符串。
     */
    private fun resolveMetadata(source: Any, key: String): String {
        val target = ProxyTarget.of(source)
        if (target !is ProxyTarget.Containerization) {
            return "null"
        }
        return target.getMetadata(key).toString()
    }

    @SubscribeEvent
    fun e(e: PlayerDamageEntityEvent) {
        if (!MythicMobsLoader.isEnable) {
            return
        }
        if (e.cause != DamageCause.of(EntityDamageEvent.DamageCause.CUSTOM)) {
            return
        }
        when (MythicMobsLoader.version.getOrNull(0)) {
            4 -> processThreatUpdateV4(e.player, e.entity, e.damage)
            5 -> processThreatUpdateV5(e.player, e.entity, e.damage)
            else -> info(
                "MythicMobDamageHandler: Unsupported MythicMobs version ${MythicMobsLoader.version.joinToString(".")}"
            )
        }
    }

    /**
     * 处理 MythicMobs 5 的威胁更新。
     *
     * @param attacker 攻击者。
     * @param entity 被攻击的实体。
     * @param damage 伤害值。
     */
    private fun processThreatUpdateV5(attacker: LivingEntity, entity: Entity, damage: Double) {
        val instance = MythicBukkit.inst().mobManager.getMythicMobInstance(entity) ?: return
        val threatTable = instance.threatTable ?: return
        threatTable.threatGain(io.lumine.mythic.bukkit.BukkitAdapter.adapt(attacker), damage)
    }

    /**
     * 处理 MythicMobs 4 的威胁更新。
     *
     * @param attacker 攻击者。
     * @param entity 被攻击的实体。
     * @param damage 伤害值。
     */
    private fun processThreatUpdateV4(attacker: LivingEntity, entity: Entity, damage: Double) {
        val instance = MythicMobs.inst().mobManager.getMythicMobInstance(entity) ?: return
        val threatTable = instance.threatTable ?: return
        threatTable.threatGain(BukkitAdapter.adapt(attacker), damage)
    }
}
