package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.util.unboxJavaToKotlin
import org.bukkit.Bukkit
import taboolib.common.platform.function.warning
import taboolib.common.util.Vector
import taboolib.common5.cbool
import taboolib.common5.cdouble
import taboolib.common5.cfloat
import taboolib.common5.cint
import taboolib.module.kether.runKether

abstract class AbstractAnimated : Animated, MetadataContainer(),Animated.Updated {

    val listeners = mutableListOf<AnimatedListener>()

    override fun listen(listener: AnimatedListener) {
        this.listeners += listener
    }

    override fun emit(event: AnimatedEvent,context: AbstractComplexScriptContext) {
        val compiled = context.compiled
        val platform = context.platform
        val options = context.createOptions {
            context { event.inject(this) }
        }
        this.listeners.filter { it.on == event.name }.forEach { listener ->
            val script = compiled.compiledScript()
            compiled.getBlockScript(listener.binding).ifPresent {
                runKether {
                    platform.run("${script.id}_${listener.binding}",script,it, options)
                }
            }
        }
    }

    override fun metaKeys(): Set<String> {
        return this.getImmutableRegistry().getKeys()
    }

    override fun getMetadata(id: String): Metadata? {
        return this[id]
    }

    override fun handleUpdate(metadata: AnimatedMeta.CoerceMeta<Any>) {
        metadata.onUpdate(this,metadata.any())
    }


    fun AbstractAnimated.text(id: String, defaultValue: String, onUpdate: Animated.(data: String) -> Unit = {}): AnimatedMeta.CoerceMeta<String> {
        return createBaked(id, defaultValue, parser = { this.toString() }, onUpdate)
    }

    fun AbstractAnimated.int(id: String, defaultValue: Int = 0, onUpdate: Animated.(data: Int) -> Unit = {}): AnimatedMeta.CoerceMeta<Int> {
        return createBaked(id, defaultValue, parser = { this.cint }, onUpdate)
    }


    fun AbstractAnimated.strictInt(id: String, defaultValue: Int = 0, min: Int = 0, onUpdate: Animated.(data: Int) -> Unit = {}): AnimatedMeta.CoerceMeta<Int> {
        val parser: Any.() -> Int = {
            val value = this.cint
            if (value < min) {
                // I didn't throw an illegal arg here. But some input should be checked.
                warning("The minimum acceptable value of $id is $min, but get $value.")
                min
            } else {
                value
            }
        }

        return createBaked(id, defaultValue, parser, onUpdate)
    }

    fun AbstractAnimated.double(id: String, defaultValue: Double = 0.0, onUpdate: Animated.(data: Double) -> Unit = {}): AnimatedMeta.CoerceMeta<Double> {
        return createBaked(id, defaultValue, parser = { this.cdouble }, onUpdate)
    }

    fun AbstractAnimated.float(id: String, defaultValue: Float = 0f, onUpdate: Animated.(data: Float) -> Unit = {}): AnimatedMeta.CoerceMeta<Float> {
        return createBaked(id, defaultValue, parser = { this.cfloat }, onUpdate)
    }


    fun AbstractAnimated.bool(id: String, defaultValue: Boolean = false, onUpdate: Animated.(data: Boolean) -> Unit = {}): AnimatedMeta.CoerceMeta<Boolean> {
        return createBaked(id, defaultValue, parser = { this.cbool }, onUpdate)
    }


    fun AbstractAnimated.vector(id: String, defaultValue: Vector, onUpdate: Animated.(data: Vector) -> Unit): AnimatedMeta.CoerceMeta<Vector> {
        return createBaked(id, defaultValue, parser = { this as Vector }, onUpdate)
    }

    fun AbstractAnimated.objective(id: String, defaultValue: LeastType): AnimatedMeta.CoerceMeta<TargetContainer> {
        return createBaked(id, TargetContainer(), parser = { this as TargetContainer })
    }

    inline fun <reified T : Any> AbstractAnimated.createBaked(id: String, defaultValue: T, noinline parser: Any.() -> T, noinline onUpdate: Animated.(data: T) -> Unit = {}): AnimatedMeta.CoerceMeta<T> {
        val unboxJavaToKotlin = unboxJavaToKotlin(T::class.java)
        val meta = AnimatedMeta.CoerceMeta(id, unboxJavaToKotlin, defaultValue, parser, onUpdate)
        this@createBaked[id] = meta
        return meta
    }


}
