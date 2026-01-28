package com.gitee.planners.module.fluxon

import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * FluxonRuntime 扩展函数
 * 支持可变参数数量的函数注册
 */
fun FluxonRuntime.registerFunction(
    name: String,
    argCounts: List<Int>,
    impl: (FluxonFunctionContext) -> Any?
) {
    for (count in argCounts) {
        val signature = if (count == 0) {
            FunctionSignature.returns(Type.OBJECT).noParams()
        } else {
            FunctionSignature.returns(Type.OBJECT).params(*Array(count) { Type.OBJECT })
        }
        registerFunction(name, signature) { ctx ->
            val result = impl(FluxonFunctionContext(ctx))
            if (result != null && result != Unit) {
                ctx.setReturnRef(result)
            }
        }
    }
}

/**
 * 包装的函数上下文，提供更简洁的 API
 */
class FluxonFunctionContext(private val ctx: org.tabooproject.fluxon.runtime.FunctionContext<*>) {

    val arguments: Array<Any?> by lazy {
        Array(ctx.argumentCount) { ctx.getRef(it) }
    }

    val environment: org.tabooproject.fluxon.runtime.Environment
        get() = ctx.environment

    fun getRef(index: Int): Any? = ctx.getRef(index)

    fun getAsInt(index: Int): Int = ctx.getAsInt(index)

    fun getAsLong(index: Int): Long = ctx.getAsLong(index)

    fun getAsDouble(index: Int): Double = ctx.getAsDouble(index)

    fun getAsString(index: Int): String? = ctx.getRef(index)?.toString()
}

/**
 * 从上下文获取玩家参数
 */
fun FluxonFunctionContext.getPlayerArg(index: Int): Player? {
    if (arguments.size > index) {
        return arguments[index] as? Player
    }
    return (environment.rootVariables["sender"] ?: environment.rootVariables["player"]) as? Player
}
