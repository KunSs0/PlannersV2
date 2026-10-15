package com.gitee.planners.module.compat.haidercore

import com.gitee.planners.core.condition.ConditionConfig
import com.gitee.planners.core.condition.ConditionExecution
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 武器熟练度原生条件：只判断累计进度对应的等级，不要求手持、不扣除经验。
 * @param reader 异步等级快照来源，生产环境由 HaiderCore 提供。
 */
class WeaponMasteryCondition(
    key: String,
    props: Map<String, Any>,
    hint: String,
    private val reader: WeaponMasteryReader = HaiderCoreCompat
) : ConditionConfig(key, props, hint, null) {
    override val expressionProperties: Boolean = false
    override val preparationKey: String = "haidercore:weapon_mastery"

    /** 原生条件无需登记 Nova 源码。 */
    override fun registerSources() {
    }

    /** 加载期严格检查合并后的字面量参数。 */
    override fun validateProperties(properties: Map<String, Any>) {
        Requirement.parse(properties)
    }

    /** 一次异步读取得到全部武器等级，随后在本次请求内复用。 */
    override fun prepare(playerId: UUID): CompletableFuture<ConditionExecution> {
        return reader.read(playerId).thenApply { levels ->
            Execution(levels)
        }
    }

    /** 原生参数，不经过 Nova 字符串表达式求值。 */
    private class Requirement(val weapon: String, val min: Int) {
        companion object {
            /** 拒绝空 ID、未知字段、非整数及溢出门槛。 */
            fun parse(properties: Map<String, Any>): Requirement {
                require(properties.keys == setOf("weapon", "min")) { "武器熟练度条件仅接受 weapon 和 min" }
                val weapon = properties["weapon"]
                val minimum = properties["min"]
                require(weapon is String && weapon.matches(Regex("[a-zA-Z0-9_-]+"))) { "weapon 必须是武器类型 ID" }
                require(minimum is Number) { "min 必须是非负整数" }
                val min = minimum.toString().toIntOrNull()
                require(min != null && min >= 0) { "min 必须是非负 Int 整数" }
                return Requirement(weapon, min)
            }
        }
    }

    /** 不可变请求快照，执行阶段不访问 HaiderCore 或数据库。 */
    private class Execution(levels: Map<String, Int>) : ConditionExecution {
        private val levels = Collections.unmodifiableMap(LinkedHashMap(levels))

        /** 精确匹配配置武器 ID；未知类型视为配置错误，不冒充零级。 */
        override fun evaluate(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): Boolean {
            val requirement = Requirement.parse(conditionProps)
            val level = levels[requirement.weapon]
            require(level != null) { "HaiderCore 未配置武器熟练度类型：${requirement.weapon}" }
            return level >= requirement.min
        }

        /** 同一快照批量判断，原生条件不产生 Nova 调用统计。 */
        override fun evaluateBatchPersistent(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionInputs: List<Map<String, Any>>): ConditionExecution.BatchEvaluation {
            val encoded = StringBuilder()
            for (input in conditionInputs) {
                val rawProps = input["props"]
                require(rawProps is Map<*, *>) { "熟练度条件缺少参数" }
                val properties = LinkedHashMap<String, Any>()
                for ((key, value) in rawProps) {
                    require(key is String && value != null) { "熟练度条件参数非法" }
                    properties[key] = value
                }
                if (evaluate(player, profile, router, route, properties)) {
                    encoded.append('1')
                } else {
                    encoded.append('0')
                }
            }
            return ConditionExecution.BatchEvaluation(encoded.toString(), null)
        }

        /** 熟练度等级只作门槛，不执行消耗。 */
        override fun consume(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>) {
        }
    }
}
