package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.attribute.AttributeProxy
import org.bukkit.entity.Entity
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info

/**
 * 将 Planners 属性来源交给明确注册的外部属性系统。
 */
interface AttributeDriver {

    companion object {

        @Volatile
        private var instance: AttributeDriver? = null

        /**
         * 注册唯一驱动，同一实例重复注册幂等，拒绝覆盖其他驱动。
         *
         * @param driver 已启用的外部属性驱动。
         */
        @Synchronized
        fun register(driver: AttributeDriver) {
            check(Bukkit.isPrimaryThread()) { "属性驱动注册必须在 Bukkit 主线程执行" }
            // 显式所有权避免重复启用覆盖另一插件的接入。
            check(driver.checkEnable()) { "属性驱动未启用: ${driver.javaClass.name}" }
            val previous = instance
            check(previous == null || previous === driver) {
                "属性驱动已注册: ${previous?.javaClass?.name}，不能覆盖为 ${driver.javaClass.name}"
            }
            if (previous === driver) {
                return
            }
            driver.onRegister()
            instance = driver
            info("AttributeDriver registered: ${driver.javaClass.name}")
            AttributeProvider.refreshOnline()
        }

        /**
         * 仅允许当前驱动释放注册，释放后不自动切换其他属性系统。
         *
         * @param driver 需要注销的所有者实例。
         */
        @Synchronized
        fun unregister(driver: AttributeDriver) {
            check(Bukkit.isPrimaryThread()) { "属性驱动注销必须在 Bukkit 主线程执行" }
            // 在清理属性源前失效队列，防止旧任务重新写入。
            if (instance !== driver) {
                return
            }
            instance = null
            AttributeProvider.invalidate()
            try {
                AttributeProvider.clear(driver)
            } finally {
                driver.onUnregister()
            }
            info("AttributeDriver unregistered: ${driver.javaClass.name}")
        }

        /**
         * 查询当前驱动，用于生命周期检查与诊断。
         *
         * @return 当前驱动，未注册时为 null。
         */
        fun current(): AttributeDriver? {
            return instance
        }

        /**
         * 初始化既有可选驱动，其他插件也可以显式注册实现。
         */
        @JvmStatic
        @Awake(LifeCycle.ENABLE)
        fun init() {
            if (instance != null) {
                return
            }
            if (SymphonyDriver.checkEnable()) {
                register(SymphonyDriver)
                return
            }
            if (AttributePlus3Driver.checkEnable()) {
                register(AttributePlus3Driver)
            }
        }

        /**
         * 获取必须存在的输出驱动，缺失时明确报错。
         *
         * @return 已注册驱动。
         */
        private fun requireCurrent(): AttributeDriver {
            // 禁止无驱动时静默丢弃职业属性。
            return checkNotNull(instance) { "Planners AttributeDriver 未注册，无法同步或读取属性；请检查属性插件启用状态。" }
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
            val driver = requireCurrent()
            // 所有驱动统一执行主线程与代际约束，外部实现只调用属性 API。
            val tokens = source.toList()
            AttributeProvider.dispatch(driver) {
                driver.set(target, id, tokens, timeout)
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
            val driver = requireCurrent()
            // 异步调用先复制输入，再统一提交到主线程。
            val tokens = source.toList()
            AttributeProvider.dispatch(driver) {
                driver.set(entity, id, tokens, timeout)
            }
        }

        /**
         * 输出已汇总的数值属性，允许驱动直接使用原生数值 Map API。
         *
         * @param entity 目标实体。
         * @param id 属性来源 ID。
         * @param values 已完成逻辑转换的属性 ID 与数值。
         * @param timeout 来源有效 tick 数，-1 表示永久。
         */
        fun setValues(entity: Entity, id: String, values: Map<String, Double>, timeout: Int) {
            val driver = requireCurrent()
            // 在接入层校验通用数值边界，不由各驱动重复实现。
            for ((key, value) in values) {
                require(value.isFinite()) { "属性 $key 必须是有限数值，实际为 $value" }
            }
            val snapshot = LinkedHashMap(values)
            AttributeProvider.dispatch(driver) {
                driver.setValues(entity, id, snapshot, timeout)
            }
        }

        /**
         * 移除目标属性
         *
         * @param target 目标
         * @param id 属性id
         */
        fun remove(target: ProxyTarget<*>, id: String) {
            val driver = requireCurrent()
            // 移除也参与相同的任务失效检查。
            AttributeProvider.dispatch(driver) {
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
            val driver = requireCurrent()
            return driver.get(entity, attrName)
        }

    }

    /**
     * 注册前的扩展钩子，仅用于驱动自身确需准备的外部资源。
     * 通用调度、在线档案补刷由 Planners 完成，驱动无需重复实现。
     */
    fun onRegister() {
        // 默认驱动只转接属性 API，无独立资源需要准备。
    }

    /**
     * 职业来源统一移除后的扩展钩子，用于释放驱动自身的外部资源。
     */
    fun onUnregister() {
        // 默认驱动无独立资源，通用清理由 Planners 完成。
    }

    /**
     * 数值输出扩展钩子，具有 Map API 的驱动可以覆盖以直接消费 Double。
     * 文本驱动使用 Planners 统一生成的十进制 token，无需各自序列化。
     *
     * @param entity 目标实体。
     * @param id 属性来源 ID。
     * @param values 物理属性 ID 与完整精度数值。
     * @param timeout 来源有效 tick 数，-1 表示永久。
     */
    fun setValues(entity: Entity, id: String, values: Map<String, Double>, timeout: Int) {
        // 文本协议的数值格式由 Planners 单点维护。
        set(entity, id, AttributeProxy.createTokens(values), timeout)
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
