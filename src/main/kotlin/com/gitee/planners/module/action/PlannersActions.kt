package com.gitee.planners.module.action

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.Registries
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.attachState
import com.gitee.planners.api.job.target.detachState
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Planners 自身提供的动作。
 *
 * 只覆盖冷却、经验、状态与共享标记四类宿主能力；战斗结算与外部插件能力
 * 由对应插件注册。
 */
object PlannersActions {

    /**
     * 设置或减少技能冷却。
     *
     * 参数 `op` 取 `set`（默认）或 `reduce`；`reduce` 表示在当前冷却上扣减。
     */
    object Cooldown : Action {

        override val id = "cooldown"

        override val aliases = listOf("cooldown")

        override fun validate(arguments: ActionArguments) {
            arguments.require("ticks")
            arguments.rejectUnknown("skill", "ticks", "op")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val ticks = arguments.requireInteger(scope, "ticks")
            if (ticks < 0) {
                return
            }
            val operation = arguments.text(scope, "op") ?: "set"
            val skillId = arguments.text(scope, "skill")
            for (target in targets) {
                val player = target.bukkitPlayer()
                if (player == null) {
                    continue
                }
                val skill = resolveSkill(scope, skillId)
                if (skill == null) {
                    continue
                }
                if (operation.equals("reduce", true)) {
                    val current = Cooler.INSTANCE.get(player, skill)
                    val remaining = current - ticks
                    Cooler.INSTANCE.set(player, skill, if (remaining < 0) 0 else remaining.toInt())
                } else {
                    Cooler.INSTANCE.set(player, skill, ticks)
                }
            }
        }

        /** 解析目标技能，未显式指定时使用当前技能。 */
        private fun resolveSkill(scope: QueryScope, skillId: String?): ImmutableSkill? {
            if (skillId != null) {
                return Registries.SKILL.getOrNull(skillId)
            }
            val skill = scope.skill
            if (skill is ImmutableSkill) {
                return skill
            }
            return null
        }
    }

    /**
     * 增加职业经验。
     */
    object Experience : Action {

        override val id = "exp"

        override val aliases = listOf("exp", "experience")

        override fun validate(arguments: ActionArguments) {
            arguments.require("amount")
            arguments.rejectUnknown("amount")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val amount = arguments.requireInteger(scope, "amount")
            if (amount == 0) {
                return
            }
            for (target in targets) {
                val player = target.bukkitPlayer()
                if (player == null) {
                    continue
                }
                PlayerTemplateAPI.addExperience(player, amount)
            }
        }
    }

    /**
     * 附加状态。
     */
    object StateAttach : Action {

        override val id = "state.attach"

        override val aliases = listOf("state.attach")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("state")
            arguments.require("ticks")
            arguments.rejectUnknown("state", "ticks", "refresh")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val stateId = arguments.requireText(scope, "state")
            val state = Registries.STATE.getOrNull(stateId)
            if (state == null) {
                throw IllegalArgumentException("${arguments.path} references unknown state '$stateId'.")
            }
            val ticks = arguments.requireInteger(scope, "ticks")
            if (ticks <= 0) {
                return
            }
            val refresh = arguments.boolean(scope, "refresh") ?: false
            for (target in targets) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                val proxy = ProxyTarget.of(entity)
                proxy.attachState(state, ticks.toLong(), refresh)
            }
        }
    }

    /**
     * 移除一层状态。
     */
    object StateDetach : Action {

        override val id = "state.detach"

        override val aliases = listOf("state.detach")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("state")
            arguments.rejectUnknown("state", "layer")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val stateId = arguments.requireText(scope, "state")
            val state = Registries.STATE.getOrNull(stateId)
            if (state == null) {
                throw IllegalArgumentException("${arguments.path} references unknown state '$stateId'.")
            }
            val layer = arguments.integer(scope, "layer") ?: 1
            if (layer <= 0) {
                return
            }
            for (target in targets) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                val proxy = ProxyTarget.of(entity)
                proxy.detachState(state, layer)
            }
        }
    }

    /**
     * 写入共享标记。
     */
    object MetaSet : Action {

        override val id = "meta.set"

        override val aliases = listOf("meta.set")

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.require("value")
            arguments.rejectUnknown("key", "value", "timeout")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val key = arguments.requireText(scope, "key")
            val value = arguments.requireText(scope, "value")
            val timeout = arguments.integer(scope, "timeout") ?: -1
            for (target in targets) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                val proxy = ProxyTarget.of(entity)
                proxy.setMetadata(key, metadataValue(value, timeout.toLong()))
            }
        }
    }

    /**
     * 读取共享标记并写入执行变量。
     *
     * 标记不存在或已清除时写入 `default`，因此后续动作可以直接用
     * `<skill.变量名>` 取值，不必为「读一次元数据」保留脚本。
     */
    object MetaGet : Action {

        override val id = "meta.get"

        override val aliases = listOf("meta.get")

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.require("to")
            arguments.rejectUnknown("key", "to", "default")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val key = arguments.requireText(scope, "key")
            val name = arguments.requireText(scope, "to")
            val fallback = arguments.number(scope, "default") ?: arguments.boolean(scope, "default")
            val variables = scope.variables
            if (variables !is MutableMap<*, *>) {
                throw IllegalStateException("${arguments.path} requires a mutable variable scope.")
            }
            var resolved: Any? = fallback
            for (target in targets) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                val proxy = ProxyTarget.of(entity)
                resolved = proxy.getMetadata(key)
                if (resolved != null) {
                    break
                }
            }
            if (resolved is String && resolved.isEmpty()) {
                resolved = fallback
            }
            if (resolved == null) {
                resolved = fallback
            }
            @Suppress("UNCHECKED_CAST")
            (variables as MutableMap<String, Any?>)[name] = resolved
        }
    }

    /**
     * 清除共享标记。
     */
    object MetaClear : Action {

        override val id = "meta.clear"

        override val aliases = listOf("meta.clear")

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.rejectUnknown("key")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val key = arguments.requireText(scope, "key")
            for (target in targets) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                val proxy = ProxyTarget.of(entity)
                // 容器没有删除入口，统一写入空字符串占位，读取方按空值视为已清除。
                proxy.setMetadata(key, metadataValue(""))
            }
        }
    }
}
