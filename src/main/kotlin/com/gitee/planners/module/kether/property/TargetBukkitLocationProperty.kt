package com.gitee.planners.module.kether.property

import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetBukkitLocation
import taboolib.common.OpenResult
import taboolib.common.platform.function.warning
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptAction
import taboolib.module.kether.ScriptProperty


@KetherProperty(TargetBukkitLocation::class)
internal fun bukkitLocationProperty() = object : ScriptProperty<TargetBukkitLocation>("operator.bukkit-location") {

    override fun read(instance: TargetBukkitLocation, key: String): OpenResult {

        return when (key) {
            "world" -> OpenResult.successful(instance.getBukkitWorld())
            "x" -> OpenResult.successful(instance.getX())
            "y" -> OpenResult.successful(instance.getY())
            "z" -> OpenResult.successful(instance.getZ())
            "yaw" -> OpenResult.successful(instance.getBukkitLocation().yaw)
            "pitch" -> OpenResult.successful(instance.getBukkitLocation().pitch)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: TargetBukkitLocation, key: String, value: Any?): OpenResult {
        val location = instance.instance
        return when (key) {
            "world" -> {
                location.world = value as org.bukkit.World
                OpenResult.successful()
            }

            "x" -> {
                location.x = value as Double
                OpenResult.successful()
            }

            "y" -> {
                location.y = value as Double
                OpenResult.successful()
            }

            "z" -> {
                location.z = value as Double
                OpenResult.successful()
            }

            "yaw" -> {
                location.yaw = value as Float
                OpenResult.successful()
            }

            "pitch" -> {
                location.pitch = value as Float
                OpenResult.successful()
            }

            else -> {
                warning("unknown key $key")
                OpenResult.failed()
            }
        }
    }

}
