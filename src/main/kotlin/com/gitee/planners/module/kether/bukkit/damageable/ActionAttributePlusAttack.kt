package com.gitee.planners.module.kether.bukkit.damageable

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.bukkit.ActionBukkitDamage.setKiller
import com.gitee.planners.module.kether.commandObjectiveOrEmpty
import com.gitee.planners.module.kether.commandObjectiveOrSender
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.AttributeHandle
import org.serverct.ersha.attribute.data.AttributeData
import org.serverct.ersha.attribute.data.AttributeSource
import taboolib.common.platform.function.warning
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser
import taboolib.platform.util.setMeta

@KetherEditor.Document("ap-attack <attribute_source:String> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]")
@CombinationKetherParser.Used
object ActionAttributePlusAttack : SimpleKetherParser("ap-attack"){
    override fun run(): QuestActionParser {
        return combinationParser {
            it.group(
                text(),
                commandObjectiveOrEmpty(),
                commandObjectiveOrSender("source")
            ).apply(it) { attribute_source,/* mode ,*/objective, source ->
                val killer = source.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.instance as? LivingEntity
                var all_damage = 0.0
                now<Double> {
                    if (killer == null) {
                        warning("Action attack source not correctly defined")
                        return@now all_damage
                    }

                    val data : AttributeData = AttributeAPI.getAttrData(killer)
                        .operationAttribute(AttributeAPI.getAttributeSource(attribute_source.split(",")),AttributeSource.OperationType.ADD,"@planners_skill")



                    objective.filterIsInstance<TargetBukkitEntity>().map { it.instance }.forEach { entity ->
                        if (killer == entity) return@forEach

                        if (entity is LivingEntity) {
                            val env = EntityDamageByEntityEvent(killer,entity, EntityDamageEvent.DamageCause.CUSTOM,0.0)
                            val handle = AttributeHandle(data,AttributeAPI.getAttrData(entity))
                                .init(env,false,true)
                                .handleAttackOrDefenseAttribute()
                            if (!env.isCancelled && !handle.isCancelled) {
                                if (handle.getDamage(killer) > entity.health) {
                                    entity.setKiller(killer)
                                    entity.setMeta("@killer",killer)
                                }

                                handle.sendAttributeMessage()
                                entity.damage(handle.getDamage(killer))
                                all_damage += handle.getDamage(killer)

                                if (handle.getDamage(entity) > 0.0)  killer.damage(handle.getDamage(entity))
                            }
                        }
                    }

                    data.takeApiAttribute("@planners_skill")
                    return@now all_damage
                }

            }
        }
    }
}
