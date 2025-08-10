package com.gitee.planners.module.kether.bukkit.damageable

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.api.event.player.TargetCapturedEvent
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.bukkit.ActionBukkitDamage.setKiller
import com.gitee.planners.module.kether.commandObjectiveOrEmpty
import com.gitee.planners.module.kether.commandObjectiveOrSender
import com.gitee.planners.module.kether.ctx
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.AttributeHandle
import org.serverct.ersha.attribute.data.AttributeData
import org.serverct.ersha.attribute.data.AttributeSource
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.util.runSync
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser
import taboolib.platform.util.setMeta

@KetherEditor.Document("ap-attack <attribute_source:String> [isolation/iso false] [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
object ActionAttributePlusAttack : SimpleKetherParser("ap-attack") {
    override fun run(): QuestActionParser {
        return combinationParser {
            it.group(
                text(),
                command("isolation", "iso", then = bool()).option().defaultsTo(false),
                commandObjectiveOrEmpty(),
                commandObjectiveOrSender("source")
            ).apply(it) { ele, isolation, objective, source ->
                val sender = source
                    .filterIsInstance<TargetBukkitEntity>()
                    .firstOrNull()?.instance as? LivingEntity
                var damage = 0.0
                now {
                    if (sender == null) {
                        warning("Action attack source not correctly defined")
                        return@now damage
                    }
                    // 是否隔离，如果隔离，则不会读取攻击者的属性
                    val data: AttributeData = if (isolation) {
                        AttributeData.create(sender)
                    } else {
                        AttributeAPI.getAttrData(sender)
                    }
                    data.operationAttribute(
                        AttributeAPI.getAttributeSource(ele.split(",")),
                        AttributeSource.OperationType.ADD,
                        "@planners_skill"
                    )

                    TargetCapturedEvent.damaged(ctx(), objective).filterIsInstance<TargetBukkitEntity>()
                        .map(TargetBukkitEntity::instance).forEach { entity ->
                            // 忽略自己攻击自己
                            if (sender == entity) {
                                return@forEach
                            }

                            if (entity is LivingEntity) {
                                val env =
                                    EntityDamageByEntityEvent(sender, entity, EntityDamageEvent.DamageCause.CUSTOM, 0.0)

                                val handle = runSync { AttributeHandle(data, AttributeAPI.getAttrData(entity)) }
                                    .init(env, false, true)
                                    .handleAttackOrDefenseAttribute()

                                if (!env.isCancelled && !handle.isCancelled) {
                                    val finalDamage = handle.getDamage(sender)
                                    if (finalDamage > entity.health) {
                                        entity.setKiller(sender)
                                        entity.setMeta("@killer", sender)
                                    }

                                    handle.sendAttributeMessage()
                                    runSync {
                                        entity.damage(finalDamage)
                                    }
                                    // 触发玩家攻击实体事件
                                    if (sender is Player) {
                                        PlayerDamageEntityEvent(
                                            sender,
                                            entity,
                                            finalDamage,
                                            EntityDamageEvent.DamageCause.CUSTOM
                                        ).call()
                                    }
                                    damage += finalDamage

                                    if (handle.getDamage(entity) > 0.0) {
                                        submit {
                                            sender.damage(handle.getDamage(entity))
                                        }
                                    }
                                }
                            }
                        }

                    data.takeApiAttribute("@planners_skill")
                    return@now damage
                }

            }
        }
    }
}
