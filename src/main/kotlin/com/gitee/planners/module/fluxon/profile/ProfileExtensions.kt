package com.gitee.planners.module.fluxon.profile

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.core.player.magic.MagicPoint.magicPoint
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionContext
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Profile 玩家资料操作扩展
 */
object ProfileExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getMagicPoint() -> int (从环境获取player)
        runtime.registerFunction("getMagicPoint", FunctionSignature.returns(Type.I).noParams()) { ctx ->
            val player = getPlayerFromEnv(ctx)
            ctx.setReturnInt(player.plannersTemplate.magicPoint)
        }

        // getMagicPoint(player) -> int
        runtime.registerFunction("getMagicPoint", FunctionSignature.returns(Type.I).params(Type.OBJECT)) { ctx ->
            val player = ctx.getRef(0) as? Player ?: return@registerFunction
            ctx.setReturnInt(player.plannersTemplate.magicPoint)
        }

        // setMagicPoint(value) -> void (从环境获取player)
        runtime.registerFunction("setMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
            val value = ctx.getAsInt(0)
            val player = getPlayerFromEnv(ctx)
            player.plannersTemplate.magicPoint = value
        }

        // setMagicPoint(value, player) -> void
        runtime.registerFunction("setMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I, Type.OBJECT)) { ctx ->
            val value = ctx.getAsInt(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            player.plannersTemplate.magicPoint = value
        }

        // takeMagicPoint(amount) -> void (从环境获取player)
        runtime.registerFunction("takeMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
            val amount = ctx.getAsInt(0)
            val player = getPlayerFromEnv(ctx)
            player.plannersTemplate.magicPoint -= amount
        }

        // takeMagicPoint(amount, player) -> void
        runtime.registerFunction("takeMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsInt(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            player.plannersTemplate.magicPoint -= amount
        }

        // giveMagicPoint(amount) -> void (从环境获取player)
        runtime.registerFunction("giveMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
            val amount = ctx.getAsInt(0)
            val player = getPlayerFromEnv(ctx)
            player.plannersTemplate.magicPoint += amount
        }

        // giveMagicPoint(amount, player) -> void
        runtime.registerFunction("giveMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsInt(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            player.plannersTemplate.magicPoint += amount
        }

        // getMaxMagicPoint() -> int (从环境获取player)
        runtime.registerFunction("getMaxMagicPoint", FunctionSignature.returns(Type.I).noParams()) { ctx ->
            val player = getPlayerFromEnv(ctx)
            ctx.setReturnInt(player.plannersTemplate["@magic.point.max"]?.asInt() ?: 0)
        }

        // getMaxMagicPoint(player) -> int
        runtime.registerFunction("getMaxMagicPoint", FunctionSignature.returns(Type.I).params(Type.OBJECT)) { ctx ->
            val player = ctx.getRef(0) as? Player ?: return@registerFunction
            ctx.setReturnInt(player.plannersTemplate["@magic.point.max"]?.asInt() ?: 0)
        }

        // setMaxMagicPoint(value) -> void (从环境获取player)
        runtime.registerFunction("setMaxMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I)) { ctx ->
            val value = ctx.getAsInt(0)
            val player = getPlayerFromEnv(ctx)
            player.plannersTemplate["@magic.point.max"] = metadataValue(value, -1)
        }

        // setMaxMagicPoint(value, player) -> void
        runtime.registerFunction("setMaxMagicPoint", FunctionSignature.returns(Type.VOID).params(Type.I, Type.OBJECT)) { ctx ->
            val value = ctx.getAsInt(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            player.plannersTemplate["@magic.point.max"] = metadataValue(value, -1)
        }
    }

    private fun getPlayerFromEnv(ctx: FunctionContext<*>): Player {
        val find = ctx.environment.rootVariables["player"]
        if (find is Player) {
            return find
        }
        throw IllegalStateException("No player found in environment")
    }
}
