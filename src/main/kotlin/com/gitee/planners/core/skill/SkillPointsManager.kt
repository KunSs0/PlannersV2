package com.gitee.planners.core.skill

import com.gitee.planners.Planners
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerLevelChangeEvent
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import taboolib.common.platform.event.SubscribeEvent

/**
 * 技能点管理器
 * <p>
 * 监听玩家等级变化，根据配置的 per-level / bonuses JS 表达式计算点数增量，
 * 直接累加到 PlayerRoute.skillPointsCurrent。
 */
object SkillPointsManager {

    private var perLevelExpr: String = "level <= 30 ? 3 : 2"
    private val bonuses = mutableMapOf<Int, String>()
    private val accumulatedCache = mutableMapOf<Int, Int>()

    /** 初始化，由 Planners.onEnable() 调用 */
    fun init() {
        reloadConfig()
    }

    /** 重新加载配置（清除缓存） */
    fun reloadConfig() {
        accumulatedCache.clear()
        bonuses.clear()
        val section = Planners.config.getConfigurationSection("settings.skill-points") ?: return
        perLevelExpr = section.getString("per-level") ?: "level <= 30 ? 3 : 2"
        section.getConfigurationSection("bonuses")?.let { bonusSection ->
            bonusSection.getKeys(false).forEach { key ->
                bonuses[key.toInt()] = bonusSection.getString(key) ?: "0"
            }
        }
    }

    // ---- 事件 ----

    @SubscribeEvent
    fun e(e: PlayerLevelChangeEvent) {
        val route = e.template.route ?: return
        val delta = calcAccumulated(e.to) - calcAccumulated(e.form)
        if (delta != 0) {
            route.addSkillPoints(delta)
        }
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun e(e: PluginReloadEvents.Post) {
        reloadConfig()
    }

    // ---- 公共 API ----

    /** 当前可用技能点 */
    fun getAvailable(route: PlayerRoute): Int = route.skillPointsCurrent

    /** 消耗技能点，返回是否成功 */
    fun takePoints(route: PlayerRoute, amount: Int): Boolean = route.takeSkillPoints(amount)

    /** 计算指定等级的累计获得点数 */
    fun calcAccumulated(level: Int): Int {
        if (level <= 0) return 0
        accumulatedCache[level]?.let { return it }

        var total = 0
        for (lv in 1..level) {
            total += evalExpr(perLevelExpr, lv)
        }
        bonuses.forEach { (bonusLv, expr) ->
            if (level >= bonusLv) {
                total += evalExpr(expr, bonusLv)
            }
        }
        accumulatedCache[level] = total
        return total
    }

    // ---- 内部 ----

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
