package com.gitee.planners.core.action.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.Target.Companion.cast
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.core.action.actionVector
import com.gitee.planners.core.action.getEnvironmentContext
import taboolib.common.OpenResult
import taboolib.common.util.Vector
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty
import taboolib.module.kether.combinationParser

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
    @KetherEditor.Document("vector new <x:Number> <y:Number> <z:Number>")
    val new = KetherHelper.combinedKetherParser("new") { parser ->
        parser.group(text(), text(), text()).apply(parser) { xStr, yStr, zStr ->
            now {
                /**
                 * 解析位置
                 * @param value 位置字符串
                 * @param relativePosition 相对位置
                 */
                fun parseLocation(value: String, relativePosition: Double = 0.0): Double {
                    return if (value.startsWith("~")) { // Use relative position
                        val loc: Double = try {
                            if (value.length == 1) 0.0 else value.substring(1).toDouble()
                        } catch (e: NumberFormatException) {
                            if (value.startsWith("~&"))
                                error("Variable ${value.substring(1)} " +
                                    "is not allowed to be used in relative position. Use vector add ... instead.")
                            error("The variable $value is invalid")
                        }
                        relativePosition + loc
                    } else // Use absolute position
                        value.toDouble()
                }

                // Get the location of the vector
                val cxtLocation = this.getEnvironmentContext().origin.cast<TargetLocation<*>>()
                val x = parseLocation(xStr,cxtLocation.getX())
                val y = parseLocation(yStr,cxtLocation.getY())
                val z = parseLocation(zStr,cxtLocation.getZ())
                Vector(x, y, z) // Return the vector
            }
        }
    }

    @KetherEditor.Document("vector copy <vector>")
    val copy = KetherHelper.combinedKetherParser("copy") { parser ->
        parser.group(actionVector()).apply(parser) { vector ->
            now {
                vector.clone()
            }
        }
    }

    @KetherEditor.Document("vector add <vector> <vector>")
    val add = KetherHelper.combinedKetherParser("add") { parser ->
        parser.group(actionVector(), actionVector()).apply(parser) { vector1, vector2 ->
            now {
                vector1.clone().add(vector2)
            }
        }
    }

}