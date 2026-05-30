package com.gitee.planners.core.skill

import com.gitee.planners.Planners
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerLevelChangeEvent
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import taboolib.common.platform.event.SubscribeEvent

object SkillPointsManager {

    private val accumulatedCache = mutableMapOf<Int, Int>()

    @SubscribeEvent
    fun e(e: PlayerLevelChangeEvent) {
        val route = e.template.route
        if (route == null) {
            return
        }
        val delta = calcAccumulated(e.to) - calcAccumulated(e.form)
        if (delta != 0) {
            route.addSkillPoints(delta)
        }
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun e(e: PluginReloadEvents.Post) {
        accumulatedCache.clear()
    }

    fun getAvailable(route: PlayerRoute): Int {
        return route.skillPointsCurrent
    }

    fun takePoints(route: PlayerRoute, amount: Int): Boolean {
        return route.takeSkillPoints(amount)
    }

    fun calcAccumulated(level: Int): Int {
        if (level <= 0) {
            return 0
        }
        val cached = accumulatedCache[level]
        if (cached != null) {
            return cached
        }

        var total = 0
        val perLevelExpr = Planners.skillPointsPerLevel.get()
        for (lv in 1..level) {
            total += evalExpr(perLevelExpr, lv)
        }
        val bonuses = Planners.skillPointsBonuses.get()
        for ((bonusKey, bonusPair) in bonuses) {
            val bonusLv = bonusPair.first
            val expr = bonusPair.second
            if (level >= bonusLv) {
                total += evalExpr(expr, bonusLv)
            }
        }
        accumulatedCache[level] = total
        return total
    }

    private fun evalExpr(expr: String, level: Int): Int {
        return try {
            val script = SingletonScript(expr)
            val options = ScriptOptions.of().set("level", level)
            script.eval(options)?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
