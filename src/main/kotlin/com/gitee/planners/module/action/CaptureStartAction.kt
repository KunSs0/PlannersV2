package com.gitee.planners.module.action

import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import java.util.UUID
import taboolib.common.platform.function.warning

/**
 * `finder.capture.start`：开启一次会话目标捕捉。
 *
 * 会话在 [ticks] 个采样周期内，把“上一采样点到当前采样点”的移动包络盒投到世界里，
 * 命中目标去重后累积；采样与结算是两条独立的钩子，业务在 `on.end` 里用
 * `@finder.capture{id=...}` 一次性结算。
 *
 * 锚点由行上的目标决定：坐标目标固定在施放点、实体目标跟随该实体、没有目标时跟随捕捉者。
 */
object CaptureStartAction : Action {

    override val id = "finder.capture.start"

    override val aliases = listOf("finder.capture.start")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("id", "dimensions", "ticks")
        arguments.rejectUnknown(
            "id", "dimensions", "offset", "end-offset", "anchor",
            "ticks", "period", "limit", "on.tick", "on.hit", "on.end",
        )
        val id = arguments.literalText("id")
        if (id != null && !CAPTURE_ID.matches(id.trim())) {
            throw IllegalArgumentException("${arguments.path} id 只允许 [A-Za-z0-9_-]+。")
        }
        val ticks = arguments.literalInteger("ticks")
        if (ticks != null && ticks <= 0) {
            throw IllegalArgumentException("${arguments.path} ticks 必须为正数。")
        }
        val period = arguments.literalInteger("period")
        if (period != null && period <= 0) {
            throw IllegalArgumentException("${arguments.path} period 必须为正数。")
        }
        val limit = arguments.literalInteger("limit")
        if (limit != null && limit <= 0) {
            throw IllegalArgumentException("${arguments.path} limit 必须为正数。")
        }
        val dimensions = arguments.literalText("dimensions")
        if (dimensions != null) {
            requireTriple(dimensions, arguments.path, "dimensions")
        }
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val id = arguments.requireText(scope, "id")
        if (!CAPTURE_ID.matches(id)) {
            throw IllegalArgumentException("${arguments.path} id 只允许 [A-Za-z0-9_-]+。")
        }
        val dimensions = parseTriple(arguments, scope, "dimensions", true)
        val offset = if (arguments.contains("offset")) {
            parseTriple(arguments, scope, "offset", false)
        } else {
            doubleArrayOf(0.0, 0.0, 0.0)
        }
        val endOffset = if (arguments.contains("end-offset")) {
            parseTriple(arguments, scope, "end-offset", false)
        } else {
            offset.copyOf()
        }
        val ticks = arguments.requireInteger(scope, "ticks")
        if (ticks <= 0) {
            throw IllegalArgumentException("${arguments.path} ticks 必须为正数。")
        }
        val period = arguments.integer(scope, "period") ?: 1
        if (period <= 0) {
            throw IllegalArgumentException("${arguments.path} period 必须为正数。")
        }
        val limit = arguments.integer(scope, "limit") ?: 0
        if (limit < 0) {
            throw IllegalArgumentException("${arguments.path} limit 不能为负数。")
        }
        val owner = scope.caster
        if (owner !is Entity) {
            throw IllegalStateException("${arguments.path} 需要实体作为捕捉者。")
        }
        val anchor = resolveAnchor(targets, owner)
        val anchorStart = anchorLocation(anchor)
        if (anchorStart == null) {
            throw IllegalStateException("${arguments.path} 无法确定捕捉锚点。")
        }
        val runner = scope.flowRunner
        val instance = CaptureSession(
            instance = UUID.randomUUID(),
            id = id,
            ownerId = owner.uniqueId,
            dimensions = dimensions,
            limit = limit,
            ticks = ticks,
            period = period,
            expiresAt = System.currentTimeMillis() + ticks.toLong() * period.toLong() * MILLIS_PER_TICK,
        )
        instance.previous = anchorStart
        instance.previousOffset = offset.copyOf()
        instance.captured = scope
        instance.runner = runner
        instance.tickFlow = arguments.text(scope, "on.tick")
        instance.hitFlow = arguments.text(scope, "on.hit")
        instance.endFlow = arguments.text(scope, "on.end")
        CaptureRegistry.register(instance)
        if (runner == null) {
            // 没有流程入口时仍保留实例，供后续行直接读取（纯采样场景）。
            return
        }
        schedule(instance, anchor, owner, offset, endOffset, scope, runner)
    }

    /**
     * 安排采样任务。
     *
     * @param session 捕捉实例。
     * @param anchor 锚点。
     * @param owner 捕捉者。
     * @param offset 常态中心偏移。
     * @param endOffset 末次采样的中心偏移。
     * @param captured 启动时捕获的作用域。
     * @param runner 流程跳转入口。
     */
    private fun schedule(
        session: CaptureSession,
        anchor: Anchor,
        owner: Entity,
        offset: DoubleArray,
        endOffset: DoubleArray,
        captured: QueryScope,
        runner: (String, QueryScope) -> Unit,
    ) {
        val generation = ActionScheduler.currentGeneration()
        var handle: ActionTask? = null
        handle = ActionScheduler.scheduleRepeating(session.period.toLong(), session.period.toLong(), generation) {
            val task = handle
            if (task == null) {
                return@scheduleRepeating
            }
            if (session.isExpired(System.currentTimeMillis()) || !CaptureRegistry.ownerValid(session.ownerId)) {
                finish(session, anchorLocation(anchor))
                task.cancel()
                return@scheduleRepeating
            }
            val current = anchorLocation(anchor)
            if (current == null) {
                finish(session, null)
                task.cancel()
                return@scheduleRepeating
            }
            val previous = session.previous
            if (previous == null) {
                session.previous = current
                return@scheduleRepeating
            }
            val world = current.world
            if (world == null || previous.world !== world) {
                finish(session, current)
                task.cancel()
                return@scheduleRepeating
            }
            val yaw = resolveYaw(owner, current)
            val currentOffset = if (session.elapsed + 1 >= session.ticks) endOffset else offset
            val finder = CaptureEnvelope.finderFor(
                previous,
                current,
                yaw,
                session.dimensions,
                session.previousOffset,
                currentOffset,
                emptyList(),
            )
            val candidates = ArrayList<Entity>()
            for (entity in world.getNearbyEntities(finder.samplingBounds())) {
                if (entity !is LivingEntity) {
                    continue
                }
                if (entity.uniqueId == session.ownerId) {
                    continue
                }
                candidates.add(entity)
            }
            finder.samples = candidates
            val added = ArrayList<LivingEntity>()
            for (entity in finder.request()) {
                if (entity !is LivingEntity) {
                    continue
                }
                if (session.add(ActionTarget.EntityTarget(entity))) {
                    added.add(entity)
                }
            }
            session.previous = current
            session.previousOffset = currentOffset
            session.elapsed += 1
            invokeTick(session, captured, runner, current)
            invokeHit(session, captured, runner, added)
            if (session.reachedLimit()) {
                session.closed = true
            }
            if (session.closed || session.elapsed >= session.ticks) {
                finish(session, current)
                task.cancel()
            }
        }
        session.task = handle
    }

    /**
     * 触发周期回调。
     *
     * @param session 捕捉实例。
     * @param captured 捕获的作用域。
     * @param runner 流程跳转入口。
     * @param anchor 本次锚点。
     */
    private fun invokeTick(session: CaptureSession, captured: QueryScope, runner: (String, QueryScope) -> Unit, anchor: Location) {
        val flow = session.tickFlow
        if (flow == null) {
            return
        }
        invoke(runner, flow, CaptureScopes.tick(captured, session, anchor), session.id)
    }

    /**
     * 触发命中回调。
     *
     * @param session 捕捉实例。
     * @param captured 捕获的作用域。
     * @param runner 流程跳转入口。
     * @param added 本次新捕捉到的目标。
     */
    private fun invokeHit(
        session: CaptureSession,
        captured: QueryScope,
        runner: (String, QueryScope) -> Unit,
        added: List<LivingEntity>,
    ) {
        val flow = session.hitFlow
        if (flow == null) {
            return
        }
        for (target in added) {
            invoke(runner, flow, CaptureScopes.hit(captured, session, target), session.id)
        }
    }

    /**
     * 结束捕捉：先跑结束回调，再注销实例。
     *
     * @param session 捕捉实例。
     * @param anchor 最后一帧锚点。
     */
    private fun finish(session: CaptureSession, anchor: Location?) {
        val captured = session.captured
        val runner = session.runner
        val flow = session.endFlow
        if (captured != null && runner != null && flow != null && anchor != null) {
            invoke(runner, flow, CaptureScopes.end(captured, session, anchor), session.id)
        }
        CaptureRegistry.remove(session.instance)
    }

    /**
     * 执行一次回调，异常只记录不中断任务。
     *
     * @param runner 流程跳转入口。
     * @param flow 流程名。
     * @param scope 回调作用域。
     * @param id 捕捉分类名。
     */
    private fun invoke(runner: (String, QueryScope) -> Unit, flow: String, scope: QueryScope, id: String) {
        try {
            runner.invoke(flow, scope)
        } catch (exception: Exception) {
            warning("Action capture '$id' failed in '$flow': " + exception.message)
        }
    }

    /**
     * 解析锚点：坐标目标固定、实体目标跟随、没有目标时跟随捕捉者。
     *
     * @param targets 行上解析出的目标。
     * @param owner 捕捉者。
     * @return 锚点。
     */
    private fun resolveAnchor(targets: List<ActionTarget>, owner: Entity): Anchor {
        for (target in targets) {
            if (target is ActionTarget.LocationTarget) {
                return Anchor(target.location.clone(), null)
            }
            val entity = target.bukkitEntity()
            if (entity != null) {
                return Anchor(null, entity)
            }
        }
        return Anchor(null, owner)
    }

    /**
     * 取锚点当前坐标。
     *
     * @param anchor 锚点。
     * @return 坐标；跟随实体失效时返回 null。
     */
    private fun anchorLocation(anchor: Anchor): Location? {
        val fixed = anchor.fixed
        if (fixed != null) {
            return fixed.clone()
        }
        val follow = anchor.follow
        if (follow == null) {
            return null
        }
        if (!follow.isValid) {
            return null
        }
        return follow.location
    }

    /**
     * 解析采样朝向。
     *
     * @param owner 捕捉者。
     * @param current 本次采样坐标。
     * @return 朝向。
     */
    private fun resolveYaw(owner: Entity, current: Location): Float {
        if (owner is LivingEntity) {
            return EntityFacingProviders.getFacingYaw(owner)
        }
        return current.yaw
    }

    /**
     * 解析三元组参数。
     *
     * @param arguments 动作参数。
     * @param scope 执行上下文。
     * @param key 参数名。
     * @param positive 是否要求每项为正数。
     * @return 三元组。
     */
    private fun parseTriple(arguments: ActionArguments, scope: QueryScope, key: String, positive: Boolean): DoubleArray {
        val parts = arguments.list(scope, key)
        requireTriple(arguments.requireText(scope, key), arguments.path, key)
        val result = DoubleArray(3)
        for (index in 0 until 3) {
            val value = parts[index].toDoubleOrNull()
            if (value == null) {
                throw IllegalArgumentException("${arguments.path} $key 第 ${index + 1} 项不是数值：${parts[index]}")
            }
            if (positive && value <= 0.0) {
                throw IllegalArgumentException("${arguments.path} $key 第 ${index + 1} 项必须为正数。")
            }
            result[index] = value
        }
        return result
    }

    /**
     * 校验三元组文本形态。
     *
     * @param text 参数文本。
     * @param path 配置路径。
     * @param key 参数名。
     */
    private fun requireTriple(text: String, path: String, key: String) {
        val parts = text.split(',')
        if (parts.size != 3) {
            throw IllegalArgumentException("$path $key 需要 3 个逗号分隔的数值，例如 1.6,1.6,1.4。")
        }
    }

    /** 每 tick 的毫秒数。 */
    private const val MILLIS_PER_TICK = 50L

    /** 捕捉分类名允许的字符。 */
    private val CAPTURE_ID = Regex("^[A-Za-z0-9_-]+$")

    /**
     * 锚点：固定坐标或跟随实体。
     *
     * @property fixed 固定坐标；跟随型为 null。
     * @property follow 跟随实体；固定型为 null。
     */
    private class Anchor(val fixed: Location?, val follow: Entity?)
}
