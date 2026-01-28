package com.gitee.planners.module.fluxon.profile

import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Profile (PlayerTemplate) 扩展注册
 */
object ProfileExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        FluxonRuntime.getInstance().registerExtension(PlayerTemplate::class.java)
            .function("level", returns(Type.I).noParams()) { ctx ->
                ctx.setReturnInt(ctx.getTarget()?.level ?: 0)
            }
            .function("exp", returns(Type.I).noParams()) { ctx ->
                ctx.setReturnInt(ctx.getTarget()?.experience ?: 0)
            }
            .function("experienceMax", returns(Type.I).noParams()) { ctx ->
                ctx.setReturnInt(ctx.getTarget()?.experienceMax ?: 0)
            }
            .function("magicPoint", returns(Type.I).noParams()) { ctx ->
                ctx.setReturnInt(ctx.getTarget()?.magicPoint ?: 0)
            }
            .function("setMagicPoint", returns(Type.VOID).params(Type.I)) { ctx ->
                ctx.getTarget()?.let { it.magicPoint = ctx.getInt(0) }
            }
            .function("takeMagicPoint", returns(Type.VOID).params(Type.I)) { ctx ->
                ctx.getTarget()?.let { it.magicPoint -= ctx.getInt(0) }
            }
            .function("giveMagicPoint", returns(Type.VOID).params(Type.I)) { ctx ->
                ctx.getTarget()?.let { it.magicPoint += ctx.getInt(0) }
            }
            .function("maxMagicPoint", returns(Type.I).noParams()) { ctx ->
                ctx.setReturnInt(ctx.getTarget()?.get("magic.point.max")?.asInt() ?: 0)
            }
            .function("job", returns(Type.STRING).noParams()) { ctx ->
                ctx.setReturnRef(ctx.getTarget()?.route?.getJob()?.id)
            }
    }
}
