package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.job.target.ProxyTarget
import org.bukkit.entity.Entity
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info

interface AttributeDriver {

    companion object {

        lateinit var INSTANCE: AttributeDriver

        @JvmStatic
        @Awake(LifeCycle.ENABLE)
        fun init() {
            if (::INSTANCE.isInitialized) {
                return
            }
            if (AttributePlus3Driver.checkEnable()) {
                INSTANCE = AttributePlus3Driver
                info("AttributeDriver load driver: AttributePlus3Driver")
            }
        }

        private fun current(): AttributeDriver? {
            if (!::INSTANCE.isInitialized) {
                return null
            }
            return INSTANCE
        }

        /**
         * 设置目标属性
         *
         * @param target 目标
         * @param id 属性id
         * @param source 属性源
         * @param timeout 超时时间
         */
        fun set(target: ProxyTarget<*>, id: String, source: List<String>, timeout: Int) {
            val driver = current()
            if (driver != null) {
                driver.set(target, id, source, timeout)
            }
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
            val driver = current()
            if (driver != null) {
                driver.set(entity, id, source, timeout)
            }
        }

        /**
         * 移除目标属性
         *
         * @param target 目标
         * @param id 属性id
         */
        fun remove(target: ProxyTarget<*>, id: String) {
            val driver = current()
            if (driver != null) {
                driver.remove(target, id)
            }
        }

        /**
         * 获取实体属性值
         *
         * @param entity 实体
         * @param attrName 属性名
         * @return 属性值数组，不存在返回空列表
         */
        fun get(entity: Entity, attrName: String): List<Double> {
            val driver = current()
            if (driver == null) {
                return emptyList()
            }
            return driver.get(entity, attrName)
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
    fun set(target: ProxyTarget<*>, id: String, source: List<String>, timeout: Int)

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
    fun remove(target: ProxyTarget<*>, id: String)

    /**
     * 获取实体属性值
     *
     * @param entity 实体
     * @param attrName 属性名
     * @return 属性值数组，不存在返回空列表
     */
    fun get(entity: Entity, attrName: String): List<Double>

}
