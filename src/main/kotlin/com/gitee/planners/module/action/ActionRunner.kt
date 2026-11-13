package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning

/**
 * 动作执行器。
 *
 * 先求值流程条件，再按声明顺序执行动作行；带触发器的行在普通流程执行时被跳过，
 * 只等事件派发或定时器到期后执行。运行期异常只记录配置路径并中止当前流程，
 * 不向事件总线抛出。
 */
object ActionRunner {

    /**
     * 流程调用的最大嵌套深度，防止 `call` 循环引用。
     */
    private const val MAX_DEPTH = 16

    /**
     * 执行一个命名流程。
     *
     * @param spec 所属 Action 结构。
     * @param flowId 流程名。
     * @param scope 执行上下文。
     * @return 实际执行的动作行数。
     * @throws IllegalArgumentException 流程不存在时抛出。
     */
    fun run(spec: ActionSpec, flowId: String, scope: QueryScope): Int {
        return runFlow(spec, flowId, scope, 0)
    }

    /**
     * 派发一次事件触发。
     *
     * 扫描全部流程中匹配该触发器且流程条件成立的行动作。
     *
     * @param spec 所属 Action 结构。
     * @param triggerId 触发器主键。
     * @param scope 执行上下文。
     * @return 实际执行的动作行数。
     */
    fun fire(spec: ActionSpec, triggerId: String, scope: QueryScope): Int {
        var executed = 0
        for (flow in spec.flows.values) {
            if (!conditionsMatch(flow, scope)) {
                continue
            }
            for (line in flow.actions) {
                if (!line.matchesTrigger(triggerId)) {
                    continue
                }
                // 触发器可按本次派发载荷筛选行；未声明参数或未覆盖 matches 时恒命中。
                val trigger = line.trigger
                val triggerArguments = line.triggerArguments
                if (trigger != null && triggerArguments != null && !trigger.matches(scope, triggerArguments)) {
                    continue
                }
                if (executeLine(line, spec, scope, 0)) {
                    executed += 1
                }
            }
        }
        return executed
    }

    /**
     * 执行一条带触发器的动作行。
     *
     * 供定时器与事件派发复用：先校验所属流程的条件，再执行该行。
     *
     * @param spec 所属 Action 结构。
     * @param flow 该行所属流程。
     * @param line 待执行的动作行。
     * @param scope 执行上下文。
     * @return 已执行时返回 true。
     */
    fun runTriggeredLine(
        spec: ActionSpec,
        flow: ActionFlow,
        line: ActionLine,
        scope: QueryScope,
    ): Boolean {
        if (!conditionsMatch(flow, scope)) {
            return false
        }
        return executeLine(line, spec, scope, 0)
    }

    /**
     * 执行一个流程，受嵌套深度限制。
     *
     * @param spec 所属 Action 结构。
     * @param flowId 流程名。
     * @param scope 执行上下文。
     * @param depth 当前调用深度。
     * @return 实际执行的动作行数。
     */
    private fun runFlow(spec: ActionSpec, flowId: String, scope: QueryScope, depth: Int): Int {
        val flow = spec.flow(flowId)
        if (flow == null) {
            throw IllegalArgumentException("Unknown action flow '$flowId' in skill '${spec.skillId}'.")
        }
        if (depth > MAX_DEPTH) {
            throw IllegalStateException("Action call depth exceeded at flow '$flowId'.")
        }
        if (!conditionsMatch(flow, scope)) {
            return 0
        }
        return runLines(flow, spec, scope, 0, depth)
    }

    /**
     * 从指定下标开始执行流程中的立即动作。
     *
     * @param flow 当前流程。
     * @param spec 所属 Action 结构。
     * @param scope 执行上下文。
     * @param start 起始下标。
     * @param depth 当前调用深度。
     * @return 实际执行的动作行数。
     */
    private fun runLines(flow: ActionFlow, spec: ActionSpec, scope: QueryScope, start: Int, depth: Int): Int {
        var executed = 0
        var index = start
        while (index < flow.actions.size) {
            val line = flow.actions[index]
            if (line.hasTrigger()) {
                index += 1
                continue
            }
            if (line.action.id == BuiltinActions.Delay.id) {
                val ticks = line.arguments.requireInteger(scope, "ticks", "t")
                if (ticks <= 0) {
                    index += 1
                    continue
                }
                scheduleNext(flow, spec, scope, index + 1, depth, ticks)
                return executed
            }
            if (!executeLine(line, spec, scope, depth)) {
                return executed
            }
            executed += 1
            index += 1
        }
        return executed
    }

    /**
     * 安排延迟后继续执行剩余动作。
     *
     * @param flow 当前流程。
     * @param spec 所属 Action 结构。
     * @param scope 执行上下文。
     * @param next 下一个待执行下标。
     * @param depth 当前调用深度。
     * @param ticks 延迟 tick。
     */
    private fun scheduleNext(
        flow: ActionFlow,
        spec: ActionSpec,
        scope: QueryScope,
        next: Int,
        depth: Int,
        ticks: Int,
    ) {
        if (!ActionScheduler.isInstalled()) {
            throw IllegalStateException(
                "Action '${flow.path}' uses delay but no scheduler is installed.",
            )
        }
        val generation = ActionScheduler.currentGeneration()
        ActionScheduler.schedule(ticks.toLong(), generation, Runnable {
            runLines(flow, spec, scope, next, depth)
        })
    }

    /**
     * 执行单行动作。
     *
     * 流程调用属于编排语义，其错误（未知流程、深度超限）直接抛出，便于加载期
     * 之外的配置问题立即暴露；其余动作的运行期异常只记录并中止当前流程。
     *
     * @param line 动作行。
     * @param spec 所属 Action 结构。
     * @param scope 执行上下文。
     * @param depth 当前调用深度。
     * @return 已执行时返回 true；目标为空或出现异常时返回 false。
     */
    private fun executeLine(line: ActionLine, spec: ActionSpec, scope: QueryScope, depth: Int): Boolean {
        if (line.action.id == BuiltinActions.Call.id) {
            return executeCall(line, spec, scope, depth)
        }
        try {
            val targets = resolveTargets(line, scope)
            if (targets.isEmpty() && !line.action.acceptsEmptyTargets) {
                return false
            }
            val scoped = scope.withFlowRunner { flowId, child ->
                runFlow(spec, flowId, child, depth + 1)
            }.withLineTargets(targets)
            line.action.execute(scoped, line.arguments, targets)
            return true
        } catch (exception: Exception) {
            warning("Action ${line.path} failed in skill '${spec.skillId}': ${exception.message}")
            return false
        }
    }

    /**
     * 执行一条流程调用。
     *
     * 没有目标器时整段只跑一次；写了目标器就按容器逐目标展开，每个目标都在
     * 绑定该目标的子上下文里执行子流程，因此子流程里的 `target.*` 就是当前元素。
     *
     * @param line 动作行。
     * @param spec 所属 Action 结构。
     * @param scope 执行上下文。
     * @param depth 当前调用深度。
     * @return 执行过至少一次时返回 true。
     */
    private fun executeCall(line: ActionLine, spec: ActionSpec, scope: QueryScope, depth: Int): Boolean {
        val flowId = line.arguments.requireText(scope, "func", "f")
        val targetSkill = line.arguments.text(scope, "skill")
        if (targetSkill != null && !targetSkill.equals(spec.skillId, true)) {
            return executeCrossSkillCall(line, scope, depth, targetSkill, flowId)
        }
        if (line.targeters.isEmpty()) {
            runFlow(spec, flowId, scope, depth + 1)
            return true
        }
        val targets = resolveTargets(line, scope)
        if (targets.isEmpty()) {
            return false
        }
        for (target in targets) {
            val entity = target.bukkitEntity()
            if (entity == null) {
                // 坐标目标：把该坐标绑定给子流程，子流程里的 @this 即这个坐标。
                val location = if (target is ActionTarget.LocationTarget) target.location else null
                if (location == null) {
                    runFlow(spec, flowId, scope, depth + 1)
                    continue
                }
                runFlow(spec, flowId, scope.withTarget(location), depth + 1)
                continue
            }
            if (entity is Player) {
                // 玩家目标：重算该玩家的技能等级与变量，子流程里 <skill.*> 即此人的值。
                val child = ActionScopeFactory.forSpecWithVariables(spec, entity, scope.payload, scope.caster)
                runFlow(spec, flowId, child, depth + 1)
                continue
            }
            runFlow(spec, flowId, scope.withTarget(entity), depth + 1)
        }
        return true
    }

    /**
     * 执行一次跨技能流程调用。
     *
     * 与同技能调用的区别是「谁提供上下文」：这里**保持原施法者**，
     * 只把技能换成目标技能，因此 `<skill.*>` 读的是施法者在该技能下的值，
     * 而每个目标通过 `target` 绑定，用于「技能 7 落点给友方回血」这类联动。
     *
     * @param line 动作行。
     * @param scope 当前执行上下文。
     * @param depth 当前调用深度。
     * @param skillId 目标技能 ID。
     * @param flowId 目标技能里的流程名。
     * @return 执行过至少一次时返回 true。
     */
    private fun executeCrossSkillCall(
        line: ActionLine,
        scope: QueryScope,
        depth: Int,
        skillId: String,
        flowId: String,
    ): Boolean {
        val other = com.gitee.planners.api.Registries.SKILL.getOrNull(skillId)
        val otherSpec = if (other == null) null else other.actionSpec
        if (otherSpec == null) {
            warning("Action ${line.path} references skill '$skillId' without a structured action.")
            return false
        }
        val caster = scope.sender
        if (caster !is Player) {
            return false
        }
        val base = ActionScopeFactory.forSpecWithVariables(otherSpec, caster, scope.payload, scope.caster)
        if (line.targeters.isEmpty()) {
            runFlow(otherSpec, flowId, base, depth + 1)
            return true
        }
        val targets = resolveTargets(line, scope)
        if (targets.isEmpty()) {
            return false
        }
        for (target in targets) {
            val entity = target.bukkitEntity()
            if (entity == null) {
                continue
            }
            runFlow(otherSpec, flowId, base.withTarget(entity), depth + 1)
        }
        return true
    }

    /**
     * 按书写顺序解析目标容器。
     *
     * 无目标器时使用施法者自身作为默认目标。
     *
     * @param line 动作行。
     * @param scope 执行上下文。
     * @return 目标容器；过滤后为空时返回空列表。
     */
    private fun resolveTargets(line: ActionLine, scope: QueryScope): List<ActionTarget> {
        if (line.targeters.isEmpty()) {
            return defaultTargets(scope)
        }
        var container: List<ActionTarget> = emptyList()
        for (step in line.targeters) {
            container = step.apply(scope, container)
            if (container.isEmpty()) {
                return emptyList()
            }
        }
        return container
    }

    /**
     * 生成无目标器时的默认目标。
     *
     * @param scope 执行上下文。
     * @return 施法者对应的目标；非实体施法者返回空列表。
     */
    private fun defaultTargets(scope: QueryScope): List<ActionTarget> {
        val sender = scope.sender
        if (sender is Player) {
            return listOf(ActionTarget.PlayerTarget(sender))
        }
        if (sender is LivingEntity) {
            return listOf(ActionTarget.EntityTarget(sender))
        }
        if (sender is Location) {
            return listOf(ActionTarget.LocationTarget(sender))
        }
        return emptyList()
    }

    /**
     * 判断流程条件是否全部成立。
     *
     * @param flow 当前流程。
     * @param scope 执行上下文。
     * @return 全部成立时返回 true。
     */
    private fun conditionsMatch(flow: ActionFlow, scope: QueryScope): Boolean {
        for (condition in flow.conditions) {
            if (!condition.matches(scope)) {
                return false
            }
        }
        return true
    }
}
