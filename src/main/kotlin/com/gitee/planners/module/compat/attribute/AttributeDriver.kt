package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.job.target.Target
import org.bukkit.entity.Entity
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.library.reflex.ReflexClass

interface AttributeDriver {

    companion object {

        private val INSTANCES = mutableListOf<AttributeDriver>()

        /**
         * 设置目标属性
         *
         * @param target 目标
         * @param id 属性id
         * @param source 属性源
         * @param timeout 超时时间
         */
        fun set(target: Target<*>, id: String, source: List<String>, timeout: Int) {
            INSTANCES.forEach { it.set(target, id, source, timeout) }
        }

        /**
         * 设置实体属性
         *
         * @param entity 实体
         * @param id 属性id
         * @param source 属性源
         * @param timeout 超时时间
         */
        fun set(entity: Entity, id: String, source: List<String>, timeout: Int) {
            INSTANCES.forEach { it.set(entity, id, source, timeout) }
        }

        /**
         * 移除目标属性
         *
         * @param target 目标
         * @param id 属性id
         */
        fun remove(target: Target<*>, id: String) {
            INSTANCES.forEach { it.remove(target, id) }
        }

    }

    /**
     * 校验是否启用该驱动
     *
     * @return 是否启用
     */
    fun checkEnable(): Boolean

    /**
     * 设置目标属性
     *
     * @param target 目标
     * @param id 属性id
     * @param source 属性源
     * @param timeout 超时时间
     */
    fun set(target: Target<*>, id: String, source: List<String>, timeout: Int)

    /**
     * 设置实体属性
     *
     * @param entity 实体
     * @param id 属性id
     * @param source 属性源
     * @param timeout 超时时间
     */
    fun set(entity: Entity, id: String, source: List<String>, timeout: Int)

    /**
     * 移除目标属性
     *
     * @param target 目标
     * @param id 属性id
     */
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
                if ((instance as AttributeDriver).checkEnable()) {
                    INSTANCES.add(instance)
                    info("AttributeDriver load driver: ${clazz.simpleName}")
                }
            }
        }

    }

}
