package com.gitee.planners.util.builtin

import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.library.reflex.ClassField
import taboolib.library.reflex.ReflexClass
import java.util.function.Supplier

/**
 * 可被 `/pl reload` 重建的注册表。
 *
 * 加载由各自的注册表入口按业务依赖顺序显式调用，本接口只声明重载动作；
 * 实现类被 [Visitor] 在 ACTIVE 阶段登记后进入重载列表。
 */
interface AutoReloadable {

    fun onReload()


    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Load

    @Awake
    class Visitor : ClassVisitor(0) {

        override fun getLifeCycle(): LifeCycle {
            // ACTIVE 阶段晚于各插件 ENABLE，确保可选 compat provider 已完成注册。
            return LifeCycle.ACTIVE
        }

        /** 在 ACTIVE 阶段登记所有可重载 registry，加载顺序由 [Registries.init] 决定。 */
        override fun visitEnd(clazz: ReflexClass) {
            if (Builtin::class.java.isAssignableFrom(clazz.toClass()) && clazz.hasAnnotation(Load::class.java)) {
                val registry = clazz.getInstance() ?: return
                if (registry is AutoReloadable) {
                    register(registry)
                }
            }
        }

        override fun visit(field: ClassField, owner: ReflexClass) {
            if (AutoReloadable::class.java.isAssignableFrom(field.fieldType) && field.name != "INSTANCE") {
                register(field.get(owner.getInstance() ?: return) as AutoReloadable)
            }
        }

    }

    companion object {

        private val registry = mutableListOf<AutoReloadable>()

        /**
         * 登记一个可重载 registry。
         *
         * 这里只登记不加载：加载必须按业务依赖顺序进行，统一由
         * `Registries.init()` 负责，否则会先按字段声明顺序加载一遍、
         * 再由 `init()` 加载一遍，白白翻倍启动耗时。
         */
        fun register(registry: AutoReloadable) {
            if (Companion.registry.any { it === registry }) {
                return
            }
            Companion.registry += registry
        }

        fun onReload() {
            registry.forEach { it.onReload() }
        }

        /**
         * 已登记的可重载 registry 数量。
         *
         * 供启动日志核对登记结果，避免登记失败时静默失去热重载能力。
         *
         * @return 数量。
         */
        fun size(): Int {
            return registry.size
        }


    }

}
