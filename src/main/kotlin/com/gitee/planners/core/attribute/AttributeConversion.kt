package com.gitee.planners.core.attribute

import com.gitee.planners.Planners
import com.gitee.planners.api.attribute.AttributeRegistryEntry

/**
 * 属性转换引擎。
 * 从 [Planners.attributeRegistry] 读取映射表，将逻辑属性换算为物理属性。
 */
object AttributeConversion {

    /**
     * 全局转换表，运行时从 Planners.attributeRegistry 读取。
     * 支持 config reload 自动更新。
     */
    private val table: Map<String, AttributeRegistryEntry>
        get() {
            return Planners.attributeRegistry.get()
        }

    /**
     * 执行转换：逻辑属性 → 物理属性。
     *
     * @param logical 逻辑属性 Map，如 { STR: 25, INT: 5 }
     * @return 物理属性 Map，如 { ATK: 25, DEF: 12 }
     */
    fun convert(logical: Map<String, Double>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for ((logicalKey, logicalValue) in logical) {
            val entry = table[logicalKey]
            if (entry == null) {
                continue
            }
            for ((physicalKey, factor) in entry.mappings) {
                val current = result[physicalKey]
                if (current == null) {
                    result[physicalKey] = logicalValue * factor
                } else {
                    result[physicalKey] = current + logicalValue * factor
                }
            }
        }
        return result
    }
}
