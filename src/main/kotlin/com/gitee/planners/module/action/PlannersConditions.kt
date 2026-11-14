package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * Planners 自身提供的条件。
 *
 * 条件只做判断，取值统一走查询路径：`node.*` 读技能树节点等级，
 * `data.*` 读共享标记，`skill.*` 读技能字段，`profile.*` 读档案字段。
 */
object PlannersConditions {
    /**
     * 目标器链是否命中。
     *
     * 参数  是链文本，例如 @crosshair{range=20} @ally @alive；与动作行使用同一套解析，
     * 链上任意一步把容器过滤为空即不成立。供「有合适目标走 A、否则走 B」类流程分支使用。
     */
    object Targeter : ActionCondition {

        override val id = "targeter"

        override val aliases = listOf("targeter")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown("e", "targeters")
            val source = arguments.raw("e", "targeters")
            if (source == null) {
                throw IllegalArgumentException("${'$'}{arguments.path} requires argument 'e'.")
            }
        }

        override fun compile(arguments: ActionArguments): Any? {
            val source = arguments.raw("e", "targeters")
            if (source == null) {
                throw IllegalArgumentException("${'$'}{arguments.path} requires argument 'e'.")
            }
            return ActionTargeterChainParser.parseText(source, "{arguments.path}.e")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            if (compiled !is List<*>) {
                throw IllegalStateException("${'$'}{arguments.path} chain was not compiled.")
            }
            var container: List<ActionTarget> = emptyList()
            for (entry in compiled) {
                if (entry !is ActionTargeterStep) {
                    throw IllegalStateException("${'$'}{arguments.path} chain contains an invalid step.")
                }
                container = entry.apply(scope, container)
                if (container.isEmpty()) {
                    return false
                }
            }
            return container.isNotEmpty()
        }
    }

    /**
     * 技能树节点是否已学习到指定等级。
     *
     * 必须同时给出树与节点，避免跨职业误判。
     */
    object Node : ActionCondition {

        override val id = "node"

        override val aliases = listOf("node")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("tree")
            arguments.require("id")
            arguments.rejectUnknown("tree", "id", "min")
        }

        override fun compile(arguments: ActionArguments): Any? {
            val tree = arguments.requireText(EMPTY_SCOPE, "tree")
            val node = arguments.requireText(EMPTY_SCOPE, "id")
            return QueryRef.parse("node.$tree.$node", "${arguments.path}.node")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            if (compiled !is QueryRef) {
                throw IllegalStateException("${arguments.path} node condition was not compiled.")
            }
            val level = compiled.resolve(scope)
            val actual = if (level is Number) {
                level.toInt()
            } else {
                0
            }
            val minimum = arguments.integer(scope, "min") ?: 1
            return actual >= minimum
        }
    }

    /**
     * 共享标记是否等于指定值。
     *
     * 不给 `is` 时只判断标记是否存在且非空。
     */
    object Meta : ActionCondition {

        override val id = "meta"

        override val aliases = listOf("meta")

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.rejectUnknown("key", "is")
        }

        override fun compile(arguments: ActionArguments): Any? {
            val key = arguments.requireText(EMPTY_SCOPE, "key")
            return QueryRef.parse("data.$key", "${arguments.path}.meta")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            if (compiled !is QueryRef) {
                throw IllegalStateException("${arguments.path} meta condition was not compiled.")
            }
            val actual = compiled.resolveText(scope)
            if (!arguments.contains("is")) {
                return actual.isNotEmpty()
            }
            val expected = arguments.requireText(scope, "is")
            return actual == expected
        }
    }

    /**
     * 技能等级区间判断。
     */
    object Level : ActionCondition {

        override val id = "level"

        override val aliases = listOf("level")

        override fun validate(arguments: ActionArguments) {
            if (!arguments.contains("min") && !arguments.contains("max")) {
                throw IllegalArgumentException("${arguments.path} requires 'min' or 'max'.")
            }
            arguments.rejectUnknown("min", "max")
        }

        override fun compile(arguments: ActionArguments): Any? {
            return QueryRef.parse("skill.level", "${arguments.path}.level")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            if (compiled !is QueryRef) {
                throw IllegalStateException("${arguments.path} level condition was not compiled.")
            }
            val value = compiled.resolve(scope)
            val actual = if (value is Number) {
                value.toInt()
            } else {
                0
            }
            val minimum = arguments.integer(scope, "min")
            if (minimum != null && actual < minimum) {
                return false
            }
            val maximum = arguments.integer(scope, "max")
            if (maximum != null && actual > maximum) {
                return false
            }
            return true
        }
    }

    /**
     * 当前职业判断。
     */
    object Job : ActionCondition {

        override val id = "job"

        override val aliases = listOf("job")

        override fun validate(arguments: ActionArguments) {
            arguments.require("is")
            arguments.rejectUnknown("is")
        }

        override fun compile(arguments: ActionArguments): Any? {
            return QueryRef.parse("profile.job", "${arguments.path}.job")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            if (compiled !is QueryRef) {
                throw IllegalStateException("${arguments.path} job condition was not compiled.")
            }
            val actual = compiled.resolveText(scope)
            val expected = arguments.requireText(scope, "is")
            return actual.equals(expected, true)
        }
    }

    /**
     * 目标生命比例是否不高于阈值。
     *
     * 优先判断当前目标，无目标时判断施法者自身。
     */
    object Wounded : ActionCondition {

        override val id = "wounded"

        override val aliases = listOf("wounded")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("ratio")
            arguments.rejectUnknown("ratio")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            val entity = resolveEntity(scope)
            if (entity == null) {
                return false
            }
            val threshold = arguments.requireNumber(scope, "ratio")
            if (threshold <= 0.0) {
                return false
            }
            val maximum = entity.maxHealth
            if (maximum <= 0.0) {
                return false
            }
            return entity.health / maximum <= threshold
        }

        /** 取目标实体，没有目标时退回施法者。 */
        private fun resolveEntity(scope: QueryScope): LivingEntity? {
            val target = scope.target
            if (target is LivingEntity) {
                return target
            }
            val sender = scope.sender
            if (sender is LivingEntity) {
                return sender
            }
            return null
        }
    }

    /**
     * 编译期读取字面量参数时使用的最小上下文。
     *
     * 这些参数在加载期必须是字面量，因此不依赖任何运行期数据。
     */
    private val EMPTY_SCOPE = QueryScope(label = "compile")
}
