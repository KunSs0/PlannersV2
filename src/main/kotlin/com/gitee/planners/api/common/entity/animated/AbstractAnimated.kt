package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.core.action.context.AbstractComplexScriptContext
import com.gitee.planners.core.action.context.Context
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.util.unboxJavaToKotlin
import org.bukkit.event.Event
import taboolib.common.util.Vector
import taboolib.common5.cbool
import taboolib.common5.cdouble
import taboolib.common5.cfloat
import taboolib.common5.cint
import taboolib.module.kether.runKether

abstract class AbstractAnimated : Animated, MetadataContainer() {

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
        return this.valuesOf().keys
    }

    override fun getMetadata(id: String): Metadata? {
        return this[id]
    }

    protected fun handleUpdate() {
        this.valuesOf().values.filterIsInstance<AnimatedMeta<Any>>().forEach {
            it.onUpdate(this, it.any())
        }
    }

    fun AbstractAnimated.text(id: String, defaultValue: String, onUpdate: Animated.(data: String) -> Unit) =
        createBaked(id, defaultValue, parser = { this.toString() }, onUpdate)

    fun AbstractAnimated.int(id: String, defaultValue: Int = 0, onUpdate: Animated.(data: Int) -> Unit) =
        createBaked(id, defaultValue, parser = { this.cint }, onUpdate)

    fun AbstractAnimated.double(id: String, defaultValue: Double = 0.0, onUpdate: Animated.(data: Double) -> Unit) =
        createBaked(id, defaultValue, parser = { this.cdouble }, onUpdate)

    fun AbstractAnimated.float(id: String, defaultValue: Float = 0f, onUpdate: Animated.(data: Float) -> Unit) =
        createBaked(id, defaultValue, parser = { this.cfloat }, onUpdate)

    fun AbstractAnimated.bool(id: String, defaultValue: Boolean = false, onUpdate: Animated.(data: Boolean) -> Unit) =
        createBaked(id, defaultValue, parser = { this.cbool }, onUpdate)

    fun AbstractAnimated.vector(
        id: String,
        defaultValue: Vector,
        onUpdate: Animated.(data: Vector) -> Unit
    ): AnimatedMeta.CoerceMeta<Vector> {
        return createBaked(id, defaultValue, parser = { this as Vector }, onUpdate)
    }

    fun AbstractAnimated.objective(id: String, defaultValue: LeastType): AnimatedMeta.CoerceMeta<TargetContainer> {
        return createBaked(id, TargetContainer(), parser = { this as TargetContainer }) {

        }
    }

    fun AnimatedMeta.CoerceMeta<Vector>.asVector(): Vector {
        return this.any() as Vector
    }

    inline fun <reified T : Any> AbstractAnimated.createBaked(
        id: String,
        defaultValue: T,
        noinline parser: Any.() -> T,
        noinline onUpdate: Animated.(data: T) -> Unit
    ): AnimatedMeta.CoerceMeta<T> {
        return AnimatedMeta.CoerceMeta(id, unboxJavaToKotlin(T::class.java), defaultValue, parser, onUpdate).apply {
            this@createBaked[id] = this
        }
    }


}
