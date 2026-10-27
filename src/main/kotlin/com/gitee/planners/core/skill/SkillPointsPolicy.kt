package com.gitee.planners.core.skill

import com.gitee.planners.core.value.IntValueSpec
import taboolib.library.configuration.ConfigurationSection

/**
 * 技能点发放规则，数值与条件结构由 Planners 自有的 [IntValueSpec] 解析。
 *
 * 每一级发放与指定等级的一次性奖励由本规则独立持有。
 */
class SkillPointsPolicy private constructor(
    private val perLevel: IntValueSpec,
    private val bonuses: Map<Int, IntValueSpec>,
) {

    /**
     * 从 Lv1 开始逐级累计，并叠加已经达到等级的阶段奖励。
     *
     * @param level 当前等级。
     * @return 累计应发技能点。
     * @throws IllegalArgumentException 发放量为负数或累计溢出时抛出。
     */
    fun accumulated(level: Int): Int {
        var total = 0
        for (currentLevel in 1..level) {
            total = add(total, resolve(perLevel, currentLevel, "$PATH.per-level"))
        }
        for ((bonusLevel, value) in bonuses) {
            if (level >= bonusLevel) {
                total = add(total, resolve(value, bonusLevel, "$PATH.bonuses.$bonusLevel"))
            }
        }
        return total
    }

    /**
     * 以正在计算的等级判断修正条件，不读取玩家最终等级或其他动态状态。
     *
     * @param value 整数规格。
     * @param level 本次奖励对应等级。
     * @param path 配置路径。
     * @return 非负技能点数量。
     */
    private fun resolve(value: IntValueSpec, level: Int, path: String): Int {
        try {
            val result = value.resolve(level) { condition, context -> condition.matches(context) }
            require(result >= 0) { "技能点不能为负数" }
            return result
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("$path 在等级 $level 求值失败：${error.message}", error)
        }
    }

    /**
     * 检查累计值范围，拒绝整数溢出。
     *
     * @param total 已累计点数。
     * @param amount 本次点数。
     * @return 相加后的点数。
     */
    private fun add(total: Int, amount: Int): Int {
        require(total.toLong() + amount <= Int.MAX_VALUE) { "$PATH 累计技能点超出 Int 范围" }
        return total + amount
    }

    companion object {

        private const val PATH = "settings.skill-points"

        /**
         * 加载每级发放与阶段奖励，只接受等级条件且不声明动态刷新。
         *
         * @param section 技能点配置节。
         * @return 完整校验后的发放规则。
         * @throws IllegalArgumentException 字段、等级、条件或数值格式非法时抛出。
         */
        fun parse(section: ConfigurationSection): SkillPointsPolicy {
            for (key in section.getKeys(false)) {
                require(key == "per-level" || key == "bonuses") { "$PATH 包含未知字段 '$key'" }
            }
            val perLevel = parseValue(section.get("per-level"), "$PATH.per-level")
            val rawBonuses = section.getConfigurationSection("bonuses")
            require(rawBonuses != null) { "$PATH.bonuses 必须是等级到整数规格的映射，可显式配置为空对象" }
            val bonuses = LinkedHashMap<Int, IntValueSpec>()
            for (key in rawBonuses.getKeys(false)) {
                val level = key.toIntOrNull()
                require(level != null && level > 0) { "$PATH.bonuses.$key 必须是正整数等级" }
                require(!bonuses.containsKey(level)) { "$PATH.bonuses.$key 等级重复" }
                bonuses[level] = parseValue(rawBonuses.get(key), "$PATH.bonuses.$key")
            }
            return SkillPointsPolicy(perLevel, bonuses)
        }

        /**
         * 复用整数规格解析器，并限制技能点的取值范围与刷新声明。
         *
         * @param raw 原始配置。
         * @param path 配置路径。
         * @return 已校验的整数规格。
         */
        private fun parseValue(raw: Any?, path: String): IntValueSpec {
            val value = if (raw is ConfigurationSection) {
                raw.toMap()
            } else {
                raw
            }
            val spec = IntValueSpec.parse(value, path)
            require(spec.base >= 0) { "$path.base 的技能点不能为负数" }
            require(spec.update.isEmpty()) { "$path.update 不支持动态刷新，技能点仅随等级和配置重载重新计算" }
            return spec
        }
    }
}
