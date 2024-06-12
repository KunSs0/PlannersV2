package com.gitee.planners.module.kether.property

import com.gitee.planners.api.job.target.TargetBukkitEntity
import org.bukkit.entity.LivingEntity
import taboolib.common.OpenResult
import taboolib.common.platform.function.warning
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty

@KetherProperty(TargetBukkitEntity::class)
internal fun bukkitEntityProperty() = object : ScriptProperty<TargetBukkitEntity>("operator.bukkit-entity") {
    override fun read(instance: TargetBukkitEntity, key: String): OpenResult {
        val entity = instance.instance
        when (key) {
            "id", "entityId" -> {
                return OpenResult.successful(entity.entityId)
            }

            "name" -> {
                return OpenResult.successful(entity.name)
            }

            "type" -> {
                return OpenResult.successful(entity.type)
            }

            "uuid" -> {
                return OpenResult.successful(entity.uniqueId)
            }

            "customName" -> {
                return OpenResult.successful(entity.customName)
            }

            "customNameVisible" -> {
                return OpenResult.successful(entity.isCustomNameVisible)
            }

            "health" -> {
                if (entity !is LivingEntity) {
                    warning("entity is not a living entity.")
                    return OpenResult.failed()
                }
                return OpenResult.successful(entity.health)
            }

            "maxHealth" -> {
                if (entity !is LivingEntity) {
                    warning("entity is not a living entity.")
                    return OpenResult.failed()
                }
                return OpenResult.successful(entity.maxHealth)
            }

            "isDead" -> {
                if (entity !is LivingEntity) {
                    warning("entity is not a living entity.")
                    return OpenResult.failed()
                }
                return OpenResult.successful(entity.isDead)
            }

            "isGlowing" -> {
                return OpenResult.successful(entity.isGlowing)
            }

            "isInvulnerable" -> {
                return OpenResult.successful(entity.isInvulnerable)
            }

            "isSilent" -> {
                return OpenResult.successful(entity.isSilent)
            }

            "isPersistent" -> {
                return OpenResult.successful(entity.isPersistent)
            }

            "isCustomNameVisible" -> {
                return OpenResult.successful(entity.isCustomNameVisible)
            }

            "entity", "instance" -> {
                return OpenResult.successful(entity)
            }

            "location" -> {
                return OpenResult.successful(entity.location)
            }

            "world" -> {
                return OpenResult.successful(entity.world)
            }

            else -> {
                warning("Unknown key: $key")
                return OpenResult.failed()
            }

        }
    }

    override fun write(instance: TargetBukkitEntity, key: String, value: Any?): OpenResult {
        val entity = instance.instance
        return when (key) {

            "customName" -> {
                entity.customName = value as String
                OpenResult.successful()
            }

            "customNameVisible" -> {
                entity.isCustomNameVisible = value as Boolean
                OpenResult.successful()
            }

            "health" -> {
                if (entity !is LivingEntity) {
                    warning("entity is not a living entity.")
                    return OpenResult.failed()
                }
                entity.health = value as Double
                OpenResult.successful()
            }

            "maxHealth" -> {
                if (entity !is LivingEntity) {
                    warning("entity is not a living entity.")
                    return OpenResult.failed()
                }
                entity.maxHealth = value as Double
                OpenResult.successful()
            }

            "isGlowing" -> {
                entity.isGlowing = value as Boolean
                OpenResult.successful()
            }

            "isInvulnerable" -> {
                entity.isInvulnerable = value as Boolean
                OpenResult.successful()
            }

            "isSilent" -> {
                entity.isSilent = value as Boolean
                OpenResult.successful()
            }

            "isPersistent" -> {
                entity.isPersistent = value as Boolean
                OpenResult.successful()
            }

            "isCustomNameVisible" -> {
                entity.isCustomNameVisible = value as Boolean
                OpenResult.successful()
            }

            else -> {
                warning("Unknown key: $key")
                OpenResult.failed()
            }

        }
    }
}
