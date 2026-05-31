package com.gitee.planners.core.attribute

import com.gitee.planners.Planners
import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.module.compat.attribute.AttributeDriver
import org.bukkit.entity.LivingEntity

/**
 * 属性代理中心。
 * 注册 [AttributeSource] → 分流（逻辑/物理）→ 转换 → 合并 → 推送 [AttributeDriver]。
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
     * 获取单个逻辑属性值（仅注册表中的 key 参与汇总）。
     */
    fun get(entity: LivingEntity, key: String): Double {
        val registry = Planners.attributeRegistry.get()
        if (!registry.containsKey(key)) {
            return 0.0
        }
        var total = 0.0
        for (source in sources) {
            val value = source.getAttributes(entity)[key]
            if (value != null) {
                total += value
            }
        }
        return total
    }

    /**
     * 获取所有逻辑属性汇总（所有来源累加）。
     */
    fun getLogical(entity: LivingEntity): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        val registry = Planners.attributeRegistry.get()
        for (source in sources) {
            for ((key, value) in source.getAttributes(entity)) {
                if (registry.containsKey(key)) {
                    val current = result[key]
                    if (current == null) {
                        result[key] = value
                    } else {
                        result[key] = current + value
                    }
                }
            }
        }
        return result
    }

    /**
     * 重算并推送到外部属性插件。
     *
     * 流程：
     * ① 收集各来源属性 → 按 registry 分流（逻辑 / 物理直通）
     * ② 逻辑属性走 [AttributeConversion.convert] 转换
     * ③ 转换结果与物理直通合并
     * ④ 推送 [AttributeDriver.set]
     */
    fun sync(entity: LivingEntity) {
        val registry = Planners.attributeRegistry.get()
        val logical = mutableMapOf<String, Double>()
        val physical = mutableMapOf<String, Double>()

        // ① 收集 + 分流
        for (source in sources) {
            for ((key, value) in source.getAttributes(entity)) {
                if (registry.containsKey(key)) {
                    val current = logical[key]
                    if (current == null) {
                        logical[key] = value
                    } else {
                        logical[key] = current + value
                    }
                } else {
                    val current = physical[key]
                    if (current == null) {
                        physical[key] = value
                    } else {
                        physical[key] = current + value
                    }
                }
            }
        }

        // ② 转换：逻辑 → 物理
        val converted = AttributeConversion.convert(logical)

        // ③ 合并
        for ((key, value) in converted) {
            val current = physical[key]
            if (current == null) {
                physical[key] = value
            } else {
                physical[key] = current + value
            }
        }

        // ④ 推送
        val list = physical.map { (k, v) ->
            "$k: +${v.toInt()}"
        }
        AttributeDriver.set(entity, "planners-proxy", list, -1)
    }
}
