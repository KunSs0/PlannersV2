package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.util.RunningClassRegistriesVisitor.Companion.toClass
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.library.reflex.ReflexClass

interface AttributeDriver {

    companion object {

        private val INSTANCES = mutableListOf<AttributeDriver>()

        fun set(target: Target<*>, id: String, source: List<String>, timeout: Int) {
            INSTANCES.forEach { it.set(target, id, source, timeout) }
        }

        fun remove(target: Target<*>, id: String) {
            INSTANCES.forEach { it.remove(target, id) }
        }

    }

    /**
     * 校验是否启用该驱动
     * @return 是否启用
     */
    fun authorizeEnable(): Boolean

    fun set(target: Target<*>, id: String, source: List<String>, timeout: Int)

    fun remove(target: Target<*>, id: String)

    @Awake
    class Visitor : ClassVisitor(0) {

        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.ACTIVE
        }

        @Suppress("NAME_SHADOWING")
        override fun visitEnd(clazz: ReflexClass) {

            if (AttributeDriver::class.java.isAssignableFrom(clazz.toClass())) {
                val instance = clazz.getInstance() ?: return
                if ((instance as AttributeDriver).authorizeEnable()) {
                    INSTANCES.add(instance)
                    info("AttributeDriver load driver: ${clazz.simpleName}")
                }
            }
        }

    }

}
