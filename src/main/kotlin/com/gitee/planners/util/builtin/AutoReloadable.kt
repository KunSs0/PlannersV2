package com.gitee.planners.util.builtin

import com.gitee.planners.util.RunningClassRegistriesVisitor.Companion.toClass
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.library.reflex.ClassField
import taboolib.library.reflex.ReflexClass
import java.util.function.Supplier

interface AutoReloadable {

    fun onLoad()

    fun onReload()


    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Load

    @Awake
    class Visitor : ClassVisitor(0) {

        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.ENABLE
        }

        /** 在 LOAD 阶段 唤醒 */
        override fun visitEnd(clazz: ReflexClass) {
            if (Builtin::class.java.isAssignableFrom(clazz.toClass()) && clazz.hasAnnotation(Load::class.java)) {
                val registry = clazz.getInstance() ?: return
                if (registry is AutoReloadable) {
                    visit(registry)
                }
            }
        }

        override fun visit(field: ClassField, owner: ReflexClass) {
            if (AutoReloadable::class.java.isAssignableFrom(field.fieldType) && field.name != "INSTANCE") {
                visit(field.get(owner.getInstance() ?: return) as AutoReloadable)
            }
        }

    }

    companion object {

        private val registry = mutableListOf<AutoReloadable>()

        fun visit(registry: AutoReloadable) {
            registry.onLoad()
            Companion.registry += registry
        }

        fun onReload() {
            registry.forEach { it.onReload() }
        }


    }

}
