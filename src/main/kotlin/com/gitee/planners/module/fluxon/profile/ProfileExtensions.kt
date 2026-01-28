package com.gitee.planners.module.fluxon.profile

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.module.fluxon.FluxonFunctionContext
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Profile 玩家资料操作扩展
 */
object ProfileExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getMagicPoint([player]) -> int
        runtime.registerFunction("getMagicPoint", listOf(0, 1)) { ctx ->
            ctx.getPlayerArg(0).plannersTemplate.magicPoint
        }

        // setMagicPoint(value, [player]) -> void
        runtime.registerFunction("setMagicPoint", listOf(1, 2)) { ctx ->
            val value = (ctx.arguments[0] as Number).toInt()
            ctx.getPlayerArg(1).plannersTemplate.magicPoint = value
            null
        }

        // takeMagicPoint(amount, [player]) -> void
        runtime.registerFunction("takeMagicPoint", listOf(1, 2)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toInt()
            ctx.getPlayerArg(1).plannersTemplate.magicPoint -= amount
            null
        }

        // giveMagicPoint(amount, [player]) -> void
        runtime.registerFunction("giveMagicPoint", listOf(1, 2)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toInt()
            ctx.getPlayerArg(1).plannersTemplate.magicPoint += amount
            null
        }

        // getMaxMagicPoint([player]) -> int
        runtime.registerFunction("getMaxMagicPoint", listOf(0, 1)) { ctx ->
            ctx.getPlayerArg(0).plannersTemplate["@magic.point.max"]?.asInt() ?: 0
        }

        // setMaxMagicPoint(value, [player]) -> void
        runtime.registerFunction("setMaxMagicPoint", listOf(1, 2)) { ctx ->
            val value = (ctx.arguments[0] as Number).toInt()
            ctx.getPlayerArg(1).plannersTemplate["@magic.point.max"] = metadataValue(value, -1)
            null
        }
    }

    private fun FluxonFunctionContext.getPlayerArg(index: Int): Player {
        if (arguments.size > index) {
            return arguments[index] as? Player
                ?: throw IllegalStateException("Argument at $index is not a player")
        }
        return environment.rootVariables["player"] as? Player
            ?: throw IllegalStateException("No player found in environment")
    }
}
