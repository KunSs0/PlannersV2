package com.gitee.planners.module.kether.math

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.Target.Companion.cast
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.kether.*
import com.gitee.planners.util.math.asSimpleMatrix
import taboolib.common.OpenResult
import taboolib.common.util.Vector
import taboolib.common5.cdouble
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty
import kotlin.math.cos

@KetherEditor.Document("vector ...")
@CombinationKetherParser.Used
object ActionVector : MultipleKetherParser("vector") {

    @KetherProperty(bind = Vector::class)
    fun property() = object : ScriptProperty<Vector>("math.vector") { // FIXME: fix id
        override fun read(instance: Vector, key: String): OpenResult {
            return when (key) {
                "x" -> OpenResult.successful(instance.x)
                "y" -> OpenResult.successful(instance.y)
                "z" -> OpenResult.successful(instance.z)
                "0" -> OpenResult.successful(instance.x)
                "1" -> OpenResult.successful(instance.y)
                "2" -> OpenResult.successful(instance.z)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: Vector, key: String, value: Any?): OpenResult {
            when (key) {
                "x" -> instance.x = value.toString().toDouble()
                "y" -> instance.y = value.toString().toDouble()
                "z" -> instance.z = value.toString().toDouble()
                "0" -> instance.x = value.toString().toDouble()
                "1" -> instance.y = value.toString().toDouble()
                "2" -> instance.z = value.toString().toDouble()
                else -> return OpenResult.failed()
            }
            return OpenResult.successful(instance)
        }
    }

    /**
     * 返回一个 Vector 对象
     * 支持 ~ 符号表示相对位置
     * 支持变量
     * 例如: vector ~1 ~2 &myVar
     */
    @Suppress("NAME_SHADOWING")
    @KetherEditor.Document("vector new <x:Number> <y:Number> <z:Number> [at objective:TargetContainer(sender)]")
    val create = KetherHelper.combinedKetherParser("new") {
        it.group(text(), text(), text(), commandObjectiveOrOrigin()).apply(it) { x, y, z, origin ->
            now {
                val origin = origin.filterIsInstance<TargetLocation<*>>().firstOrNull()
                        ?: this.getEnvironmentContext().origin.cast()
                Vector(
                        parseVector(x, origin?.getX() ?: 0.0),
                        parseVector(y, origin?.getY() ?: 0.0),
                        parseVector(z, origin?.getZ() ?: 0.0),
                ) // Return the vector
            }
        }
    }

    /**
     * 返回一个 Vector 对象, 该向量指向目标看向的位置
     *
     * 例如: vector looking-at [of objective:TargetContainer(sender)]
     * 默认目标为执行者
     */
    @KetherEditor.Document("vector looking-at|sight [of objective:TargetContainer(sender)] [scale <number>]")
    val lookingAt = KetherHelper.combinedKetherParser("looking-at", "sight") {
        it.group(commandObjectiveOrSender("of"), commandDouble("scale", 1.0)).apply(it) { targets, scale ->
            now {
                val entity = targets.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.getInstance()
                        ?: error("No entity found")
                val yaw = Math.toRadians(entity.location.yaw.toDouble())
                val pitch = Math.toRadians(entity.location.pitch.toDouble())
                Vector(
                        -kotlin.math.sin(yaw) * cos(pitch),
                        -kotlin.math.sin(pitch),
                        cos(yaw) * cos(pitch)
                ).normalize().multiply(scale)
            }
        }
    }

    @KetherEditor.Document("vector copy <vector>")
    val clone = KetherHelper.combinedKetherParser("copy") {
        it.group(actionVector()).apply(it) { vector ->
            now { vector.clone() }
        }
    }

    @KetherEditor.Document("vector add <vector> <vector>")
    val add = KetherHelper.combinedKetherParser("add") {
        it.group(actionVector(), actionVector()).apply(it) { vector1, vector2 ->
            now { vector1.clone().add(vector2) }
        }
    }

    @KetherEditor.Document("vector norm <vector>")
    val norm = KetherHelper.combinedKetherParser("norm") {
        it.group(actionVector()).apply(it) { vector ->
            now { vector.clone().normalize() }
        }
    }

    @KetherEditor.Document("vector length <vector>")
    val length = KetherHelper.combinedKetherParser("length") {
        it.group(actionVector()).apply(it) { vector ->
            now { vector.length() }
        }
    }

    @KetherEditor.Document("vector dot <vector> <vector>")
    val dot = KetherHelper.combinedKetherParser("dot") {
        it.group(actionVector(), actionVector()).apply(it) { vector1, vector2 ->
            now { vector1.dot(vector2) }
        }
    }

    @KetherEditor.Document("vector cross <vector> <vector>")
    val cross = KetherHelper.combinedKetherParser("cross", "cross-product") {
        it.group(actionVector(), actionVector()).apply(it) { vector1, vector2 ->
            now { vector1.clone().crossProduct(vector2) }
        }
    }

    @KetherEditor.Document("vector scale <vector> <number>")
    val scale = KetherHelper.combinedKetherParser("scale") {
        it.group(actionVector(), double()).apply(it) { vector, value ->
            now { vector.clone().multiply(value) }
        }
    }

    @KetherEditor.Document("vector angle <vector> <vector>")
    val angle = KetherHelper.combinedKetherParser("angle") {
        it.group(actionVector(), actionVector()).apply(it) { vector1, vector2 ->
            now { vector1.angle(vector2) }
        }
    }

    val projection = KetherHelper.combinedKetherParser("proj", "projection") {
        it.group(actionVector(), actionVector()).apply(it) { vector1, vector2 ->
            now { vector2.multiply(vector1.dot(vector2) / vector2.dot(vector2)) }
        }
    }

    @KetherEditor.Document("vector rotate <vector> <angle:radian> <axis:Vector>")
    val rotate = KetherHelper.combinedKetherParser("rotate") {
        it.group(actionVector(), double(), actionVector()).apply(it) { vector, angle, axis ->
            now { vector.clone().rotateAroundAxis(axis, angle) }
        }
    }

    @KetherEditor.Document("vector rotate <vector> <angle:radian> <axis:x|y|z>")
    val rotateOn = KetherHelper.combinedKetherParser("rotate-on") {
        it.group(actionVector(), double(), text()).apply(it) { vector, angle, axis ->
            now {
                when (axis) {
                    "x" -> vector.clone().rotateAroundX(angle)
                    "y" -> vector.clone().rotateAroundY(angle)
                    "z" -> vector.clone().rotateAroundZ(angle)
                    else -> error("Unknown axis $axis")
                }
            }
        }
    }

    @KetherEditor.Document("vector mult-matrix <vector> <matrix:Matrix>")
    val multMatrix = KetherHelper.combinedKetherParser("mult-matrix", "mult-m") {
        it.group(actionVector(), actionTransformMatrix()).apply(it) { vector, matrix ->
            now { matrix.copy().mult(vector.asSimpleMatrix()) }
        }
    }

    /**
     * 解析位置
     * @param value 位置字符串
     * @param offset 相对位置
     */
    private fun parseVector(value: String, offset: Double = 0.0): Double {
        return if (value.startsWith("~")) { // Use relative position
            val loc: Double = try {
                if (value.length == 1) 0.0 else value.substring(1).cdouble
            } catch (e: NumberFormatException) {
                if (value.startsWith("~&"))
                    error("Variable ${value.substring(1)} is not allowed to be used in relative position. Use vector add ... instead.")

                error("The variable $value is invalid")
            }
            offset + loc
        } else // Use absolute position
            value.toDouble()
    }
}
