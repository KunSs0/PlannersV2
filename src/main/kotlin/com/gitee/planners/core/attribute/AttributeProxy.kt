package com.gitee.planners.core.attribute

import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.module.compat.attribute.AttributeDriver
import org.bukkit.entity.LivingEntity
import java.math.BigDecimal

/**
 * 属性代理中心。
 * 注册 [AttributeSource] → 按物理属性 ID 累加 → 整组推送 [AttributeDriver]。
 */
object AttributeProxy {

    private val sources = sortedSetOf<AttributeSource>(
        compareBy({ it.priority }, { it.id })
    )

    /** 注册属性来源 */
    fun register(source: AttributeSource) {
        sources.add(source)
    }

    /** 注销属性来源 */
    fun unregister(source: AttributeSource) {
        sources.remove(source)
    }

    /**
     * 汇总所有来源的物理属性，同名键按来源从上到下累加。
     *
     * @param entity 目标实体。
     * @return 物理属性 ID 与完整精度数值。
     */
    fun collect(entity: LivingEntity): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (source in sources) {
            for ((key, value) in source.getAttributes(entity)) {
                val current = result[key]
                if (current == null) {
                    result[key] = value
                } else {
                    result[key] = current + value
                }
            }
        }
        return result
    }

    /**
     * 重算并推送到外部属性插件。
     *
     * 来源直接提供物理属性 ID，汇总后整组交给驱动，由驱动选择原生数值 API 或通用文本协议。
     *
     * @param entity 目标实体。
     */
    fun sync(entity: LivingEntity) {
        val physical = collect(entity)
        AttributeDriver.setValues(entity, "planners-proxy", physical, -1)
    }

    /**
     * 将物理属性转换为保留小数的通用 token，不在输出层取整。
     *
     * @param physical 已完成汇总的物理属性。
     * @return 使用普通十进制及单一正负号的属性 token。
     * @throws IllegalArgumentException 属性包含非有限数值时抛出。
     */
    internal fun createTokens(physical: Map<String, Double>): List<String> {
        // 禁止科学计数法干扰属性解析；负值直接携带负号，不生成 +-。
        val tokens = ArrayList<String>()
        for ((key, value) in physical) {
            require(value.isFinite()) { "属性 $key 必须是有限数值，实际为 $value" }
            val decimal = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
            val sign = if (value >= 0.0) {
                "+"
            } else {
                ""
            }
            tokens.add("$key: $sign$decimal")
        }
        return tokens
    }
}
