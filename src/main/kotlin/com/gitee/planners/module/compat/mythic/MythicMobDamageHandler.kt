package com.gitee.planners.module.compat.mythic

import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.api.job.target.ProxyTarget
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.skills.placeholders.Placeholder
import io.lumine.xikage.mythicmobs.skills.placeholders.PlaceholderMeta
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerLifeCycleTask
import java.util.function.BiFunction

object MythicMobDamageHandler {

    init {
        registerLifeCycleTask(LifeCycle.ENABLE) {
            if (MythicMobsLoader.isEnable) {
                if (MythicMobsLoader.version[0] == 4) {
                    MythicMobs.inst().placeholderManager.register("caster.pl.metadata", Placeholder.meta(object : BiFunction<PlaceholderMeta,String,String> {

                        override fun apply(metadata: PlaceholderMeta, args: String): String {
                            val target = ProxyTarget.of(metadata.caster.entity)
                            if (target !is ProxyTarget.Containerization) {
                                return "null"
                            }

                            return target.getMetadata(args).toString();
                        }
                    }))
                }
                // v5
                else if (MythicMobsLoader.version[0] == 5) {
                    MythicBukkit.inst().placeholderManager.register("caster.pl.metadata", io.lumine.mythic.core.skills.placeholders.Placeholder.meta(object : BiFunction<io.lumine.mythic.core.skills.placeholders.PlaceholderMeta,String,String> {

                        override fun apply(metadata: io.lumine.mythic.core.skills.placeholders.PlaceholderMeta, args: String): String {
                            val target = ProxyTarget.of(metadata.caster.entity)
                            if (target !is ProxyTarget.Containerization) {
                                return "null"
                            }

                            return target.getMetadata(args).toString();
                        }
                    }))
                }

            }
        }
    }

    @SubscribeEvent
    fun e(e: PlayerDamageEntityEvent) {
        info("MythicMobDamageHandler ")
        info(" isEnable: $MythicMobsLoader.isEnable")
        info(" damager: ${e.player}")
        if (MythicMobsLoader.isEnable && e.cause == EntityDamageEvent.DamageCause.CUSTOM) {
            // v4
            if (MythicMobsLoader.version[0] == 4) {
                processThreatUpdateV4(e.player,e.entity,e.damage)
            }
            // v5
            else if (MythicMobsLoader.version[0] == 5) {
                processThreatUpdateV5(e.player,e.entity,e.damage)
            } else {
                info("MythicMobDamageHandler: Unsupported MythicMobs version ${MythicMobsLoader.version.joinToString(".")}")
            }
        }
    }

    /**
     * 处理 MythicMob 的威胁更新
     *
     * @param attacker 攻击者
     * @param entity 被攻击的实体
     * @param damage 伤害值
     */
    private fun processThreatUpdateV5(attacker: LivingEntity, entity: Entity, damage: Double) {
        val instance = MythicBukkit.inst().mobManager.getMythicMobInstance(entity)
        info(" instance: $instance")
        if (instance == null) {
            return
        }
        if (instance.threatTable == null) {
            return
        }
        instance.threatTable.threatGain(io.lumine.mythic.bukkit.BukkitAdapter.adapt(attacker), damage)
        info("Threat gain: ${damage} by ${attacker.name} to ${entity.name}")
    }

    /**
     * 处理 MythicMob 的威胁更新
     *
     * @param attacker 攻击者
     * @param entity 被攻击的实体
     * @param damage 伤害值
     */
    private fun processThreatUpdateV4(attacker: LivingEntity,entity: Entity,damage: Double) {
        val instance = MythicMobs.inst().mobManager.getMythicMobInstance(entity)
        info(" instance: $instance")
        if (instance == null) {
            return
        }
        if (instance.threatTable == null) {
            return
        }
        instance.threatTable.threatGain(BukkitAdapter.adapt(attacker), damage)
        info("Threat gain: ${damage} by ${attacker.name} to ${entity.name}")
    }

}
