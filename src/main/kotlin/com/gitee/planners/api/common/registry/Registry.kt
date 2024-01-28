package com.gitee.planners.api.common.registry

import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.library.reflex.ClassField
import java.util.function.Supplier


interface Registry<K, V> {

    fun getOrNull(id: K): V?

    fun get(id: K): V

    operator fun set(id: K, value: V)

    fun getValues(): List<V>

    fun getKeys(): Set<K>

    fun containsKey(key: K): Boolean

    fun getSize(): Int

    fun toMap(): Map<K, V>

    fun removeAll()

    fun remove(key: K)

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Load

    @Awake
    class Visitor : ClassVisitor(0) {

        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.LOAD
        }

        /** 在 LOAD 阶段 唤醒 */
        override fun visitEnd(clazz: Class<*>, instance: Supplier<*>?) {
            if (Registry::class.java.isAssignableFrom(clazz) && clazz.isAnnotationPresent(Load::class.java)) {
                val registry = instance?.get() ?: return
                if (registry is ReloadableRegistry) {
                    ReloadableRegistry.visit(registry)
                }
            }
        }

        override fun visit(field: ClassField, clazz: Class<*>, instance: Supplier<*>?) {
            if (ReloadableRegistry::class.java.isAssignableFrom(field.fieldType) && field.name != "INSTANCE") {
                ReloadableRegistry.visit(field.get(instance?.get() ?: return) as ReloadableRegistry)
            }
        }

    }

    fun removeIf(func: (key: K, value: V) -> Boolean)
}
