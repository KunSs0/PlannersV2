package com.gitee.planners.core.skill

import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerLevelChangeEvent
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.event.player.PlayerSetRouteEvent
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.module.script.SingletonScript
import taboolib.common.platform.event.SubscribeEvent

object SkillPointsManager {

    private val calculationLock = Any()
    private val accumulatedCache = mutableMapOf<Int, Int>()
    private val expressions = LinkedHashMap<String, SingletonScript>()

    /**
     * 在 Workspace 加载前登记技能点公式的全部 Nova SourceUnit。
     */
    fun prepareSources() {
        synchronized(calculationLock) {
            expressions.clear()
            // 配置代次变化后，旧等级缓存不能参与新公式计算。
            accumulatedCache.clear()
            registerExpression(Planners.skillPointsPerLevel.get())
            val bonuses = Planners.skillPointsBonuses.get()
            for ((_, bonusPair) in bonuses) {
                registerExpression(bonusPair.second)
            }
        }
    }

    @SubscribeEvent
    fun e(e: PlayerLevelChangeEvent) {
        val router = e.template.playerRouter
        if (router == null) {
            return
        }
        val delta = calcAccumulated(e.to) - calcAccumulated(e.form)
        if (delta != 0) {
            router.addSkillPoints(delta)
        }
    }

    /**
     * 档案加载完成后补足历史等级遗漏的技能点。
     *
     * @param e 已绑定玩家档案的事件。
     */
    @SubscribeEvent
    fun e(e: PlayerProfileLoadedEvent) {
        reconcile(e.template)
    }

    /**
     * 首次选择职业或转职完成后校正技能点；新职业线从 Lv1 开始。
     *
     * @param e 职业路线创建完成的事件。
     */
    @SubscribeEvent
    fun e(e: PlayerSetRouteEvent.Post) {
        reconcile(e.template)
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun e(e: PluginReloadEvents.Post) {
        synchronized(calculationLock) {
            accumulatedCache.clear()
        }
        // 重载后立即校正在线玩家，避免旧代码已积压的等级差额继续缺失。
        val templates = PlayerTemplateAPI.values().toList()
        for (template in templates) {
            if (template.onlinePlayer.isOnline) {
                reconcile(template)
            }
        }
    }

    /**
     * 按当前等级累计值校正玩家职业线的技能点余额。
     *
     * @param template 需要校正的在线玩家档案。
     */
    private fun reconcile(template: PlayerTemplate) {
        val router = template.playerRouter
        if (router == null) {
            return
        }
        if (router.currentRouteId < 0) {
            return
        }
        val level = router.level
        router.reconcileSkillPoints(calcAccumulated(level))
    }

    fun getAvailable(router: PlayerRouter): Int {
        return router.skillPointsCurrent
    }

    fun takePoints(router: PlayerRouter, amount: Int): Boolean {
        return router.takeSkillPoints(amount)
    }

    fun calcAccumulated(level: Int): Int {
        synchronized(calculationLock) {
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
    }

    private fun evalExpr(expr: String, level: Int): Int {
        val script = expressions[expr]
        if (script == null) {
            throw IllegalStateException("The skill-points expression was not precompiled: $expr")
        }
        val result = script.eval(level)
        if (result == null) {
            throw IllegalStateException("The skill-points expression returned null: $expr")
        }
        return result.toString().toDouble().toInt()
    }

    /** 登记一条去重后的技能点公式。 */
    private fun registerExpression(expression: String) {
        if (!expressions.containsKey(expression)) {
            expressions[expression] = SingletonScript(
                expression,
                "config:skill-points:${expression.hashCode()}",
                listOf("level")
            )
        }
    }
}
