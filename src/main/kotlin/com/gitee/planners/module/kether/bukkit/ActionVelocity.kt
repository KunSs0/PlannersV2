package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.actionVector
import com.gitee.planners.module.kether.commandObjectiveOrSender
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import taboolib.common5.Coerce

@CombinationKetherParser.Used
object ActionVelocity : MultipleKetherParser("velocity") {

    // velocity spurt <vector> [at objective:TargetContainer(sender)]
    val spurt = process { entity, original, increment ->
        val vector1 = entity.location.direction.setY(0).normalize()
        val vector2 = vector1.clone().crossProduct(Vector(0, 1, 0))
        vector1.multiply(increment.z)
        vector1.add(vector2.multiply(increment.x)).y = increment.y
        original.x = vector1.x
        original.y = vector1.y
        original.z = vector1.z
    }

    @KetherEditor.Document("velocity set <vector> [at objective:TargetContainer(sender)]")
    val set = process("=") { entity, original, increment ->
        original.x = increment.x
        original.y = increment.y
        original.z = increment.z
    }

    @KetherEditor.Document("velocity add <vector> [at objective:TargetContainer(sender)]")
    val add = process("+=") { entity, original, increment ->
        original.x += increment.x
        original.y += increment.y
        original.z += increment.z
    }

    @KetherEditor.Document("velocity subtract <vector> [at objective:TargetContainer(sender)]")
    val subtract = process("sub", "-=") { entity, original, increment ->
        original.x -= increment.x
        original.y -= increment.y
        original.z -= increment.z
    }

    @KetherEditor.Document("velocity multiply <vector> [at objective:TargetContainer(sender)]")
    val multiply = process("mul", "*=") { entity, original, increment ->
        original.x *= increment.x
        original.y *= increment.y
        original.z *= increment.z
    }

    @KetherEditor.Document("velocity divide <vector> [at objective:TargetContainer(sender)]")
    val divide = process("div", "/*") { entity, original, increment ->
        original.x /= increment.x
        original.y /= increment.y
        original.z /= increment.z
    }

    @KetherEditor.Document("velocity zero [at objective:TargetContainer(sender)]")
    val zero = process { entity, original, _ ->
        original.x = 0.0
        original.y = 0.0
        original.z = 0.0
    }

    // velocity <action> <vector> [at objective:TargetContainer(sender)]
    fun process(vararg id: String, func: (entity: LivingEntity, original: Vector, increment: Vector) -> Unit) =
        KetherHelper.combinedKetherParser(*id) {
            it.group(actionVector(), commandObjectiveOrSender()).apply(it) { increment, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        val entity = it.instance as LivingEntity
                        val velocity = entity.velocity
                        func(entity, velocity, Vector(increment.x, increment.y, increment.z))
                        entity.velocity = velocity
                    }
                }
            }
        }
}
