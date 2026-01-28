package com.gitee.planners.module.fluxon.context

import com.gitee.planners.api.context.Context
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.skill.context.SkillContext
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 上下文扩展
 * 注册 ctx 对象的函数
 */
object ContextExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        // 注册 Context 基础扩展
        FluxonRuntime.getInstance().registerExtension(Context::class.java)
            .function("sender", returns(Type.OBJECT).noParams()) { ctx ->
                ctx.setReturnRef(ctx.getTarget()?.sender)
            }
            .function("origin", returns(Type.OBJECT).noParams()) { ctx ->
                ctx.setReturnRef(ctx.getTarget()?.origin)
            }
            .function("setOrigin", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                ctx.getTarget()?.origin = ctx.getRef(0) as? ProxyTarget<*>
            }

        // 注册 SkillContext 扩展
        FluxonRuntime.getInstance().registerExtension(SkillContext::class.java)
            .function("level", returns(Type.I).noParams()) { ctx ->
                ctx.setReturnInt(ctx.getTarget()?.level ?: 0)
            }
            .function("setLevel", returns(Type.VOID).params(Type.I)) { ctx ->
                ctx.getTarget()?.level = ctx.getInt(0)
            }
            .function("skill", returns(Type.OBJECT).noParams()) { ctx ->
                ctx.setReturnRef(ctx.getTarget()?.skill)
            }
    }
}
