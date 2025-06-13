package com.gitee.planners.util

import com.gitee.planners.util.builtin.Builtin
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.library.reflex.ReflexClass
import java.util.function.Supplier

abstract class RunningClassRegistriesVisitor<T>(val clazz: Class<T>, val builtin: Builtin<String, T>) : ClassVisitor(0) {

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.LOAD
    }

    override fun visitEnd(clazz: ReflexClass) {
        if (this.clazz.isAssignableFrom(clazz.toClass())) {
            this.visit(clazz.getInstance() as? T ?: return)
        }
    }

    open fun visit(instance: T) {
        builtin[getId(instance)] = instance
    }

    abstract fun getId(instance: T): String
}
