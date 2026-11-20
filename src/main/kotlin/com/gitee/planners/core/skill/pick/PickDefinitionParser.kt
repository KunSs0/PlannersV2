package com.gitee.planners.core.skill.pick

import com.gitee.planners.api.pick.DirectionPickDefinition
import com.gitee.planners.api.pick.EntityPickDefinition
import com.gitee.planners.api.pick.PickConfig
import com.gitee.planners.api.pick.PickDefinition
import com.gitee.planners.api.pick.PositionPickDefinition

/**
 * `__option__.pick` 配置解析。
 *
 * 三种 type 由 Planners 自己解析与校验：未知 type、缺失字段、未知字段、非法数值都直接报错，
 * 不做静默降级。引擎只负责表现，不再参与类型定义。
 */
object PickDefinitionParser {

    /** `position` 允许的字段。 */
    private val POSITION_KEYS = setOf("type", "max-duration-ticks", "range", "placement", "require-line-of-sight")

    /** `entity` 允许的字段。 */
    private val ENTITY_KEYS = setOf("type", "max-duration-ticks", "range", "max-angle-degrees", "require-line-of-sight", "filter", "fallback")

    /** `direction` 允许的字段。 */
    private val DIRECTION_KEYS = setOf("type", "max-duration-ticks", "max-pitch-degrees")

    /**
     * 解析一个 pick 配置段。
     *
     * @param config 配置段。
     * @param path 配置路径，用于报错定位。
     * @return 不可变定义。
     */
    fun parse(config: PickConfig, path: String): PickDefinition {
        val type = config.getString("type")
        if (type == null || type.isBlank()) {
            throw IllegalArgumentException("$path.type 不能为空")
        }
        val maxDurationTicks = readPositiveInt(config, "max-duration-ticks", path)
        return when (type.trim()) {
            PositionPickDefinition.TYPE -> parsePosition(config, path, maxDurationTicks)
            EntityPickDefinition.TYPE -> parseEntity(config, path, maxDurationTicks)
            DirectionPickDefinition.TYPE -> parseDirection(config, path, maxDurationTicks)
            else -> throw IllegalArgumentException("$path.type 未内置：$type（可用 position / entity / direction）")
        }
    }

    /**
     * 解析 `position`。
     *
     * @param config 配置段。
     * @param path 配置路径。
     * @param maxDurationTicks 会话时长。
     * @return 定义。
     */
    private fun parsePosition(config: PickConfig, path: String, maxDurationTicks: Int): PickDefinition {
        rejectUnknown(config, POSITION_KEYS, path)
        val range = readPositiveDouble(config, "range", path)
        val placement = config.getString("placement")
        if (placement == null || placement.isBlank()) {
            throw IllegalArgumentException("$path.placement 不能为空")
        }
        val requireLineOfSight = readBoolean(config, "require-line-of-sight", path)
        return PositionPickDefinition(maxDurationTicks, range, placement.trim(), requireLineOfSight)
    }

    /**
     * 解析 `entity`。
     *
     * @param config 配置段。
     * @param path 配置路径。
     * @param maxDurationTicks 会话时长。
     * @return 定义。
     */
    private fun parseEntity(config: PickConfig, path: String, maxDurationTicks: Int): PickDefinition {
        rejectUnknown(config, ENTITY_KEYS, path)
        val range = readPositiveDouble(config, "range", path)
        val maxAngleDegrees = readPositiveDouble(config, "max-angle-degrees", path)
        if (maxAngleDegrees > 180.0) {
            throw IllegalArgumentException("$path.max-angle-degrees 必须在 0..180 之间")
        }
        val requireLineOfSight = readBoolean(config, "require-line-of-sight", path)
        val fallback = readFallback(config, path)
        return EntityPickDefinition(maxDurationTicks, range, maxAngleDegrees, requireLineOfSight, fallback)
    }

    /**
     * 读取可选的兜底策略。
     *
     * 兜底策略决定“没有选中任何合规实体”时是否继续技能流程，因此未声明的策略必须报错，
     * 不能静默按空值处理。
     *
     * @param config 配置段。
     * @param path 配置路径。
     * @return 兜底策略；未声明时返回 null。
     */
    private fun readFallback(config: PickConfig, path: String): String? {
        if (!config.contains("fallback")) {
            return null
        }
        val raw = config.getString("fallback")
        if (raw == null) {
            throw IllegalArgumentException("$path.fallback 不能为空")
        }
        val value = raw.trim()
        if (value == EntityPickDefinition.FALLBACK_SELF) {
            return value
        }
        throw IllegalArgumentException("$path.fallback 未内置：$raw（可用 self）")
    }

    /**
     * 解析 `direction`。
     *
     * @param config 配置段。
     * @param path 配置路径。
     * @param maxDurationTicks 会话时长。
     * @return 定义。
     */
    private fun parseDirection(config: PickConfig, path: String, maxDurationTicks: Int): PickDefinition {
        rejectUnknown(config, DIRECTION_KEYS, path)
        val maxPitchDegrees = readPositiveDouble(config, "max-pitch-degrees", path)
        if (maxPitchDegrees > 90.0) {
            throw IllegalArgumentException("$path.max-pitch-degrees 必须在 0..90 之间")
        }
        return DirectionPickDefinition(maxDurationTicks, maxPitchDegrees)
    }

    /**
     * 校验未知字段。
     *
     * @param config 配置段。
     * @param allowed 允许的字段集合。
     * @param path 配置路径。
     */
    private fun rejectUnknown(config: PickConfig, allowed: Set<String>, path: String) {
        for (key in config.keys()) {
            if (allowed.contains(key)) {
                continue
            }
            throw IllegalArgumentException("$path 存在未知字段 '$key'，允许的字段：$allowed")
        }
    }

    /**
     * 读取必填正整数。
     *
     * @param config 配置段。
     * @param key 字段名。
     * @param path 配置路径。
     * @return 数值。
     */
    private fun readPositiveInt(config: PickConfig, key: String, path: String): Int {
        if (!config.contains(key)) {
            throw IllegalArgumentException("$path.$key 不能为空")
        }
        val value = config.getInt(key)
        if (value <= 0) {
            throw IllegalArgumentException("$path.$key 必须大于 0")
        }
        return value
    }

    /**
     * 读取必填正小数。
     *
     * @param config 配置段。
     * @param key 字段名。
     * @param path 配置路径。
     * @return 数值。
     */
    private fun readPositiveDouble(config: PickConfig, key: String, path: String): Double {
        if (!config.contains(key)) {
            throw IllegalArgumentException("$path.$key 不能为空")
        }
        val value = config.getDouble(key)
        if (!value.isFinite() || value <= 0.0) {
            throw IllegalArgumentException("$path.$key 必须为有限正数")
        }
        return value
    }

    /**
     * 读取必填布尔值。
     *
     * @param config 配置段。
     * @param key 字段名。
     * @param path 配置路径。
     * @return 布尔值。
     */
    private fun readBoolean(config: PickConfig, key: String, path: String): Boolean {
        if (!config.contains(key)) {
            throw IllegalArgumentException("$path.$key 不能为空")
        }
        return config.getBoolean(key)
    }
}
