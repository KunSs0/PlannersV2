package com.gitee.planners.module.fluxon.attributeplus

import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.AttributeHandle
import org.serverct.ersha.attribute.data.AttributeData
import org.serverct.ersha.attribute.data.AttributeSource
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.util.runSync
import taboolib.platform.util.setMeta

/**
 * AttributePlus 属性攻击扩展
 */
@Requires(classes = ["org.serverct.ersha.AttributePlus"])
object AttributePlusExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // apAttack(attributes, [isolation], [targets], [source]) - AttributePlus 属性攻击
        runtime.registerFunction("apAttack", listOf(1, 2, 3, 4)) { ctx ->
            val attributes = ctx.getAsString(0) ?: return@registerFunction 0.0
            val isolation = if (ctx.arguments.size > 1) ctx.getRef(1) as? Boolean ?: false else false
            val targetsArg = ctx.getTargetsArg(2, LeastType.EMPTY)
            val sourceArg = ctx.getTargetsArg(3, LeastType.SENDER)

            val sender = sourceArg.filterIsInstance<ProxyTarget.BukkitEntity>()
                .firstOrNull()?.instance as? LivingEntity

            if (sender == null) {
                warning("apAttack: source not correctly defined")
                return@registerFunction 0.0
            }

            // 是否隔离，如果隔离，则不会读取攻击者的属性
            val data: AttributeData = if (isolation) {
                AttributeData.create(sender)
            } else {
                AttributeAPI.getAttrData(sender)
            }

            // 添加临时属性
            data.operationAttribute(
                AttributeAPI.getAttributeSource(attributes.split(",")),
                AttributeSource.OperationType.ADD,
                "@planners_skill"
            )

            var totalDamage = 0.0

            // 获取目标
            val targets = targetsArg

            targets.filterIsInstance<ProxyTarget.BukkitEntity>()
                .map { it.instance }
                .filterIsInstance<LivingEntity>()
                .filter { it != sender }
                .forEach { entity ->
                    val env = EntityDamageByEntityEvent(
                        sender, entity,
                        EntityDamageEvent.DamageCause.CUSTOM, 0.0
                    )

                    val handle = runSync {
                        AttributeHandle(data, AttributeAPI.getAttrData(entity))
                    }.init(env, false, true).handleAttackOrDefenseAttribute()

                    if (!env.isCancelled && !handle.isCancelled) {
                        val finalDamage = handle.getDamage(sender)

                        // 设置击杀者标记
                        if (finalDamage > entity.health) {
                            entity.setMeta("@killer", sender)
                        }

                        handle.sendAttributeMessage()
                        runSync { entity.damage(finalDamage) }

                        // 触发玩家攻击实体事件
                        if (sender is Player) {
                            PlayerDamageEntityEvent(
                                sender, entity, finalDamage,
                                EntityDamageEvent.DamageCause.CUSTOM
                            ).call()
                        }

                        totalDamage += finalDamage

                        // 反伤处理
                        val reflectDamage = handle.getDamage(entity)
                        if (reflectDamage > 0.0) {
                            submit { sender.damage(reflectDamage) }
                        }
                    }
                }

            // 移除临时属性
            data.takeApiAttribute("@planners_skill")

            totalDamage
        }
    }
}
