package com.gitee.planners.api.common.registry

import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import java.util.function.Supplier

abstract class RunningClassRegistriesVisitor<T>(val target: Class<T>, val registry: Registry<String, T>) :
    ClassVisitor(0) {

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.LOAD
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitEnd(clazz: Class<*>, instance: Supplier<*>?) {
        if (this.target.isAssignableFrom(clazz)) {
            this.visit(instance?.get() as? T ?: return)
        }
    }

    open fun visit(instance: T) {
        registry[getId(instance)] = instance
    }

    abstract fun getId(instance: T): String


}
