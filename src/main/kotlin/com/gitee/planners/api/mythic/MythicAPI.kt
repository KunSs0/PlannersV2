package com.gitee.planners.api.mythic

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer
import com.gitee.planners.module.compat.mythic.MythicMobsLoader
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import taboolib.common.util.runSync

/** MythicMobs API exposed to Nova scripts. */
object MythicAPI {

    @JvmField
    val threat = ThreatApi

    /** API for updating MythicMobs threat tables. */
    object ThreatApi {

        /** Adds the given threat amount to every MythicMob in the target container. */
        fun gain(source: LivingEntity, amount: Double, targets: ProxyTargetContainer): Int {
            if (amount <= 0.0 || !amount.isFinite()) {
                throw IllegalArgumentException("Threat amount must be a finite positive number")
            }
            if (!source.isValid) {
                return 0
            }
            if (!MythicMobsLoader.isEnable) {
                return 0
            }

            return runSync {
                val majorVersion = MythicMobsLoader.version.getOrNull(0)
                if (majorVersion == null) {
                    return@runSync 0
                }

                var applied = 0
                for (target in targets) {
                    val entityTarget = target as? ProxyTarget.BukkitEntity
                    if (entityTarget == null) {
                        continue
                    }

                    val entity = entityTarget.instance
                    val success = when (majorVersion) {
                        4 -> gainV4(source, entity, amount)
                        5 -> gainV5(source, entity, amount)
                        else -> false
                    }
                    if (success) {
                        applied += 1
                    }
                }
                applied
            }
        }

        private fun gainV4(source: LivingEntity, target: Entity, amount: Double): Boolean {
            val instance = MythicMobs.inst().mobManager.getMythicMobInstance(target)
            if (instance == null) {
                return false
            }
            val threatTable = instance.threatTable
            if (threatTable == null) {
                return false
            }
            threatTable.threatGain(
                io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter.adapt(source),
                amount
            )
            return true
        }

        private fun gainV5(source: LivingEntity, target: Entity, amount: Double): Boolean {
            val instance = MythicBukkit.inst().mobManager.getMythicMobInstance(target)
            if (instance == null) {
                return false
            }
            val threatTable = instance.threatTable
            if (threatTable == null) {
                return false
            }
            threatTable.threatGain(
                io.lumine.mythic.bukkit.BukkitAdapter.adapt(source),
                amount
            )
            return true
        }
    }
}
