package com.gitee.planners.module.compat.mythic

import org.bukkit.entity.LivingEntity
import taboolib.common.util.runSync

/**
 * MythicMobs 仇恨读取。
 *
 * `target.anchored` 的语义是"施法者是目标的当前仇恨第一"，
 * 由 Planners 直接读 MythicMobs 威胁表，不再由战斗插件注入。
 */
object MythicThreatAccess {

    /**
     * 判断施法者是否为目标的当前仇恨第一。
     *
     * 目标不是 MythicMobs 生物、未开启威胁表或威胁表为空时返回 false。
     *
     * @param target 目标实体。
     * @param sender 施法者。
     * @return 施法者是当前仇恨第一时返回 true。
     */
    fun anchored(target: Any?, sender: Any?): Boolean {
        if (target !is LivingEntity || sender !is LivingEntity) {
            return false
        }
        if (!MythicMobsLoader.isEnable) {
            return false
        }
        return runSync {
            val majorVersion = MythicMobsLoader.version.getOrNull(0)
            if (majorVersion == null) {
                return@runSync false
            }
            when (majorVersion) {
                4 -> anchoredV4(target, sender)
                5 -> anchoredV5(target, sender)
                else -> false
            }
        }
    }

    /**
     * MythicMobs 4 的仇恨第一判定。
     *
     * @param target 目标实体。
     * @param sender 施法者。
     * @return 施法者是当前仇恨第一时返回 true。
     */
    private fun anchoredV4(target: LivingEntity, sender: LivingEntity): Boolean {
        val instance = io.lumine.xikage.mythicmobs.MythicMobs.inst().mobManager.getActiveMob(target.uniqueId).orElse(null)
        if (instance == null || !instance.hasThreatTable()) {
            return false
        }
        val top = instance.threatTable.topThreatHolder
        if (top == null) {
            return false
        }
        val topEntity = top.bukkitEntity
        if (topEntity == null) {
            return false
        }
        return topEntity.uniqueId == sender.uniqueId
    }

    /**
     * MythicMobs 5 的仇恨第一判定。
     *
     * @param target 目标实体。
     * @param sender 施法者。
     * @return 施法者是当前仇恨第一时返回 true。
     */
    private fun anchoredV5(target: LivingEntity, sender: LivingEntity): Boolean {
        val instance = io.lumine.mythic.bukkit.MythicBukkit.inst().mobManager.getActiveMob(target.uniqueId).orElse(null)
        if (instance == null || !instance.hasThreatTable()) {
            return false
        }
        val top = instance.threatTable.topThreatHolder
        if (top == null) {
            return false
        }
        val topEntity = top.bukkitEntity
        if (topEntity == null) {
            return false
        }
        return topEntity.uniqueId == sender.uniqueId
    }
}
