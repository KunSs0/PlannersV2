package com.gitee.planners.core.player.magic

import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI.plannersLoaded
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common5.cint
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.lazyConversion
import taboolib.platform.util.onlinePlayers

/**
 * 默认魔力值提供器。
 *
 * 定期在 Bukkit 主线程计算在线玩家的魔力上限与恢复值。脚本表达式会复用
 * GraalJS 上下文，因此不得从多个异步调度线程并发执行。
 */
class DefaultMagicPointProvider : MagicPointProvider {

    var enabled = true

    var taskUpperLimit: PlatformExecutor.PlatformTask? = null

    var taskResume: PlatformExecutor.PlatformTask? = null

    override fun getPoint(player: Player): Int {
        return player.plannersTemplate["magic.point"]?.asInt() ?: 0
    }

    override fun setPoint(player: Player, magicPoint: Int) {
        player.plannersTemplate["magic.point"] = metadataValue(minOf(magicPoint, getPointInUpperLimit(player)), -1)
    }

    override fun getPointInUpperLimit(player: Player): Int {
        return player.plannersTemplate["magic.point.max"]?.asInt() ?: 0
    }

    fun close() {
        this.taskUpperLimit?.cancel()
        this.taskResume?.cancel()
        enabled = false
    }

    fun setPointInUpperLimit(player: Player, magicPoint: Int) {
        player.plannersTemplate["magic.point.max"] = metadataValue(magicPoint, -1)
    }

    init {
        Planners.config.onReload { scheduleUpdateTasks() }
        scheduleUpdateTasks()
    }

    /**
     * 注册魔力更新定时任务。
     *
     * 每次配置重载都会先取消旧任务，再使用主线程任务重新注册。运行在主线程
     * 能保证 GraalJS 上下文串行访问，同时满足 Bukkit 玩家对象仅由主线程访问的要求。
     */
    private fun scheduleUpdateTasks() {
        if (!enabled) {
            // 已关闭时再次取消任务，避免配置重载重新注册任务。
            close()
            return
        }

        // 配置重载后替换两个定时任务。
        taskUpperLimit?.cancel()
        taskUpperLimit = submit(delay = updateTickUpperLimit, period = updateTickUpperLimit) {
            executeUpdateTaskInUpperLimit()
        }
        taskResume?.cancel()
        taskResume = submit(delay = updateTickResume, period = updateTickResume) {
            executeUpdateTask()
        }
    }

    fun executeUpdateTask() {
        for (player in onlinePlayers) {
            executeUpdateTask(player)
        }
    }

    fun executeUpdateTask(player: Player) {
        if (!player.plannersLoaded) {
            return
        }
        val data = expressionResume.get().eval(ScriptOptions.common(player))
        val template = player.plannersTemplate
        template.magicPoint += data?.cint ?: 0
    }

    /**
     * 执行更新任务
     */
    fun executeUpdateTaskInUpperLimit() {
        for (player in onlinePlayers) {
            executeUpdateTaskInUpperLimit(player)
        }
    }

    fun executeUpdateTaskInUpperLimit(player: Player) {
        if (!player.plannersLoaded) {
            return
        }
        val data = expressionUpperLimit.get().eval(ScriptOptions.common(player))
        setPointInUpperLimit(player, data?.cint ?: 0)
    }


    companion object {

        @ConfigNode("settings.magic-point.upper-limit.expression")
        val expressionUpperLimit = lazyConversion<String?, SingletonScript> {
            SingletonScript(this ?: "${Int.MAX_VALUE}")
        }

        @ConfigNode("settings.magic-point.resume.expression")
        val expressionResume = lazyConversion<String?, SingletonScript> {
            SingletonScript(this ?: "${Int.MAX_VALUE}")
        }

        @ConfigNode("settings.magic-point.upper-limit.update-tick")
        val updateTickUpperLimit = 20L

        @ConfigNode("settings.magic-point.resume.update-tick")
        val updateTickResume = 20L


    }


}
