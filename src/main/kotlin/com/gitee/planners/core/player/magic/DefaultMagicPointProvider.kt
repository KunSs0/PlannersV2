package com.gitee.planners.core.player.magic

import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common5.cint
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.lazyConversion
import taboolib.platform.util.onlinePlayers

class DefaultMagicPointProvider : MagicPointProvider {

    var enabled = true

    var taskUpperLimit: PlatformExecutor.PlatformTask? = null

    var taskResume: PlatformExecutor.PlatformTask? = null

    override fun getPoint(player: Player): Int {
        return player.plannersTemplate["@magic.point"]?.asInt() ?: 0
    }

    override fun setPoint(player: Player, magicPoint: Int) {
        player.plannersTemplate["@magic.point"] = metadataValue(minOf(magicPoint, getPointInUpperLimit(player)), -1)
    }

    override fun getPointInUpperLimit(player: Player): Int {
        return player.plannersTemplate["@magic.point.max"]?.asInt() ?: 0
    }

    fun close() {
        this.taskUpperLimit?.cancel()
        this.taskResume?.cancel()
        enabled = false
    }

    fun setPointInUpperLimit(player: Player, magicPoint: Int) {
        player.plannersTemplate["@magic.point.max"] = metadataValue(magicPoint, -1)
    }

    init {
        fun process() {
            if (!enabled) {
                // 重新关闭确保task的运行状态
                close()
                return
            }

            // 注销任务
            taskUpperLimit?.cancel()
            this.taskUpperLimit = submitAsync(delay = updateTickUpperLimit, period = updateTickUpperLimit) {
                executeUpdateTaskInUpperLimit()
            }
            taskResume?.cancel()
            this.taskResume = submitAsync(delay = updateTickResume, period = updateTickResume) {
                executeUpdateTask()
            }
        }
        Planners.config.onReload { process() }
        process()
    }

    fun executeUpdateTask() {
        onlinePlayers.forEach(this::executeUpdateTask)
    }

    fun executeUpdateTask(player: Player) {
        expressionResume.get().run(FluxonScriptOptions.common(player)).thenAccept { data ->
            val template = player.plannersTemplate
            template.magicPoint += (data?.cint ?: 0)
        }
    }

    /**
     * 执行更新任务
     */
    fun executeUpdateTaskInUpperLimit() {
        onlinePlayers.forEach(this::executeUpdateTaskInUpperLimit)
    }

    fun executeUpdateTaskInUpperLimit(player: Player) {
        expressionUpperLimit.get().run(FluxonScriptOptions.common(player)).thenAccept { data ->
            this.setPointInUpperLimit(player, data?.cint ?: 0)
        }
    }


    companion object {

        @ConfigNode("settings.magic-point.upper-limit.expression")
        val expressionUpperLimit = lazyConversion<String?, SingletonFluxonScript> {
            SingletonFluxonScript(this ?: "${Int.MAX_VALUE}")
        }

        @ConfigNode("settings.magic-point.resume.expression")
        val expressionResume = lazyConversion<String?, SingletonFluxonScript> {
            SingletonFluxonScript(this ?: "${Int.MAX_VALUE}")
        }

        @ConfigNode("settings.magic-point.upper-limit.update-tick")
        val updateTickUpperLimit = 20L

        @ConfigNode("settings.magic-point.resume.update-tick")
        val updateTickResume = 20L


    }


}
