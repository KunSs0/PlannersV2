package com.gitee.planners.module.fluxon.math

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import kotlin.math.*
import kotlin.random.Random

/**
 * 数学函数扩展
 * 通过 Number 类扩展实现数学函数
 */
object MathExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Double 类型数学函数扩展
        runtime.registerExtension(Double::class.java)
            .function("abs", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val value = ctx.target ?: return@function
                ctx.setReturnDouble(kotlin.math.abs(value))
            }
            .function("sqrt", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val value = ctx.target ?: return@function
                ctx.setReturnDouble(sqrt(value))
            }
            .function("sin", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val value = ctx.target ?: return@function
                ctx.setReturnDouble(sin(value))
            }
            .function("cos", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val value = ctx.target ?: return@function
                ctx.setReturnDouble(cos(value))
            }
            .function("tan", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val value = ctx.target ?: return@function
                ctx.setReturnDouble(tan(value))
            }
            .function("max", FunctionSignature.returns(Type.D).params(Type.D)) { ctx ->
                val a = ctx.target ?: return@function
                val b = ctx.getAsDouble(0)
                ctx.setReturnDouble(kotlin.math.max(a, b))
            }
            .function("min", FunctionSignature.returns(Type.D).params(Type.D)) { ctx ->
                val a = ctx.target ?: return@function
                val b = ctx.getAsDouble(0)
                ctx.setReturnDouble(kotlin.math.min(a, b))
            }

        // 全局随机数函数
        runtime.registerExtension(String::class.java)
            .function("random", FunctionSignature.returns(Type.I).params(Type.I, Type.I)) { ctx ->
                val target = ctx.target ?: return@function
                if (target != "Math" && target != "math") return@function

                val min = ctx.getAsInt(0)
                val max = ctx.getAsInt(1)
                ctx.setReturnInt(Random.nextInt(min, max + 1))
            }
    }
}
