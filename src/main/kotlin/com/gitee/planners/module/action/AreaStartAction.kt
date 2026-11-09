package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import java.util.UUID
import taboolib.common.platform.function.warning

/**
 * `area.start`：建立一个区域实例。
 *
 * 每次执行都会新建一份实例，不存在隐式替换：同一分类下同一区域主可以同时存在多份，
 * 业务要结束旧的实例时显式调用 `area.stop`。
 *
 * 圆心由行上的目标决定：坐标目标 → 固定在施放点；实体目标 → 跟随该实体；没有目标 → 固定在施法者位置。
 * 给了 `on.run` 就按 `period` 周期回调该流程，回调里 `sender` 是区域主、`target` 是锚点、
 * `area.self.*` 是本实例。
 */
object AreaStartAction : Action {

    override val id = "area.start"

    override val aliases = listOf("area.start")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.require("radius")
        arguments.require("ticks")
        arguments.rejectUnknown("id", "radius", "ticks", "height", "period", "on.run")

        val id = arguments.literalText("id")
        if (id != null && !AREA_ID.matches(id.trim())) {
            throw IllegalArgumentException("${arguments.path} id 只允许 [A-Za-z0-9_-]+。")
        }
        val radius = literalNumber(arguments, "radius")
        if (radius != null && radius <= 0.0) {
            throw IllegalArgumentException("${arguments.path} radius 必须为正数。")
        }
        val ticks = arguments.literalInteger("ticks")
        if (ticks != null && ticks <= 0) {
            throw IllegalArgumentException("${arguments.path} ticks 必须为正数：区域没有永久语义。")
        }
        val height = literalNumber(arguments, "height")
        if (height != null && height < 0.0) {
            throw IllegalArgumentException("${arguments.path} height 不能为负数。")
        }
        val hasPeriod = arguments.contains("period")
        val hasRun = arguments.contains("on.run")
        if (hasPeriod != hasRun) {
            throw IllegalArgumentException("${arguments.path} period 与 on.run 必须成对出现。")
        }
        val period = arguments.literalInteger("period")
        if (period != null && period <= 0) {
            throw IllegalArgumentException("${arguments.path} period 必须为正数。")
        }
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val id = arguments.requireText(scope, "id")
        if (!AREA_ID.matches(id)) {
            throw IllegalArgumentException("${arguments.path} id 只允许 [A-Za-z0-9_-]+。")
        }
        val radius = arguments.requireNumber(scope, "radius")
        if (radius <= 0.0) {
            throw IllegalArgumentException("${arguments.path} radius 必须为正数。")
        }
        val ticks = arguments.requireInteger(scope, "ticks")
        if (ticks <= 0) {
            throw IllegalArgumentException("${arguments.path} ticks 必须为正数：区域没有永久语义。")
        }
        val height = arguments.number(scope, "height")
        if (height != null && height < 0.0) {
            throw IllegalArgumentException("${arguments.path} height 不能为负数。")
        }
        val owner = scope.caster
        if (owner !is Entity) {
            throw IllegalStateException("${arguments.path} 需要玩家作为区域主。")
        }
        val anchor = resolveAnchor(targets, owner)
        if (anchor == null) {
            throw IllegalStateException("${arguments.path} 无法确定区域圆心。")
        }
        val now = System.currentTimeMillis()
        val instance = AreaInstance(
            instance = UUID.randomUUID(),
            id = id,
            ownerId = owner.uniqueId,
            radius = radius,
            height = height ?: DEFAULT_HEIGHT,
            fixedCenter = anchor.fixed,
            followSource = anchor.follow,
            expiresAt = now + ticks.toLong() * MILLIS_PER_TICK,
        )
        AreaRegistry.register(instance)
        if (!arguments.contains("on.run")) {
            return
        }
        val period = arguments.requireInteger(scope, "period")
        if (period <= 0) {
            throw IllegalArgumentException("${arguments.path} period 必须为正数。")
        }
        val flow = arguments.requireText(scope, "on.run")
        val runner = scope.flowRunner
        if (runner == null) {
            throw IllegalStateException("${arguments.path} on.run 只能在技能流程里使用。")
        }
        schedule(instance, scope, runner, flow, period)
    }

    /**
     * 解析锚点：坐标目标固定、实体目标跟随、没有目标时固定在施法者位置。
     *
     * @param targets 行上解析出的目标。
     * @param owner 区域主。
     * @return 锚点；无法确定时返回 null。
     */
    private fun resolveAnchor(targets: List<ActionTarget>, owner: Entity): Anchor? {
        for (target in targets) {
            if (target is ActionTarget.LocationTarget) {
                return Anchor(target.location.clone(), null)
            }
            val entity = target.bukkitEntity()
            if (entity is LivingEntity) {
                return Anchor(null, entity)
            }
        }
        return Anchor(owner.location.clone(), null)
    }

    /**
     * 安排周期回调。
     *
     * @param instance 区域实例。
     * @param captured 启动时捕获的作用域。
     * @param runner 流程跳转入口。
     * @param flow 回调流程名。
     * @param period 回调周期（tick）。
     */
    private fun schedule(
        instance: AreaInstance,
        captured: QueryScope,
        runner: (String, QueryScope) -> Unit,
        flow: String,
        period: Int,
    ) {
        val generation = ActionScheduler.currentGeneration()
        var handle: ActionTask? = null
        handle = ActionScheduler.scheduleRepeating(period.toLong(), period.toLong(), generation) {
            val task = handle
            if (task == null) {
                return@scheduleRepeating
            }
            if (isFinished(instance)) {
                instance.cancelTask()
                AreaRegistry.remove(instance.instance)
                return@scheduleRepeating
            }
            val child = AreaScopes.tick(captured, instance)
            if (child == null) {
                instance.cancelTask()
                AreaRegistry.remove(instance.instance)
                return@scheduleRepeating
            }
            try {
                runner.invoke(flow, child)
            } catch (exception: Exception) {
                warning("Action area '" + instance.id + "' tick failed in '" + flow + "': " + exception.message)
            }
        }
        instance.task = handle
    }

    /**
     * 判断实例是否已失效。
     *
     * @param instance 区域实例。
     * @return 到期、锚点不可用或区域主不在线时返回 true。
     */
    private fun isFinished(instance: AreaInstance): Boolean {
        if (instance.isExpired(System.currentTimeMillis())) {
            return true
        }
        if (instance.center() == null) {
            return true
        }
        return !AreaRegistry.ownerOnline(instance.ownerId)
    }

    /**
     * 读取字面量数值参数。
     *
     * @param arguments 动作参数。
     * @param key 参数名。
     * @return 字面量数值；参数是表达式或缺失时返回 null。
     */
    private fun literalNumber(arguments: ActionArguments, key: String): Double? {
        val text = arguments.literalText(key)
        if (text == null) {
            return null
        }
        return text.trim().toDoubleOrNull()
    }

    /** 默认上下容差。 */
    private const val DEFAULT_HEIGHT = 2.0

    /** 每 tick 的毫秒数。 */
    private const val MILLIS_PER_TICK = 50L

    /** 区域分类名允许的字符；不含点，避免与查询路径分隔符冲突。 */
    private val AREA_ID = Regex("^[A-Za-z0-9_-]+$")

    /**
     * 锚点：固定坐标或跟随实体。
     *
     * @property fixed 固定圆心；跟随型为 null。
     * @property follow 跟随实体；固定型为 null。
     */
    private class Anchor(val fixed: Location?, val follow: LivingEntity?)
}
