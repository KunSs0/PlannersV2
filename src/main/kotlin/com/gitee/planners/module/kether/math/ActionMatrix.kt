package com.gitee.planners.module.kether.math

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.module.kether.actionTransformMatrix
import com.gitee.planners.module.kether.actionVector
import com.gitee.planners.util.math.createIdentityMatrix
import com.gitee.planners.util.math.rotate
import com.gitee.planners.util.math.scale
import com.gitee.planners.util.math.translate


@KetherEditor.Document("matrix ...")
@CombinationKetherParser.Used
object ActionMatrix : MultipleKetherParser("matrix") {

    @KetherEditor.Document("matrix of|preset <matrix> ...")
    val preset = object : MultipleKetherParser("preset", "of") {

        @KetherEditor.Document("matrix of|preset identity")
        val identity = KetherHelper.simpleKetherNow("identity") {
            createIdentityMatrix()
        }

        @KetherEditor.Document("matrix of|preset translation <vector>")
        val translation = KetherHelper.combinedKetherParser("translation") {
            it.group(actionVector()).apply(it) { vector ->
                now {
                    createIdentityMatrix().translate(vector.x, vector.y, vector.z)
                }
            }
        }

        @KetherEditor.Document("matrix of|preset rotation <angle:radian> <axis:Vector>")
        val rotation = KetherHelper.combinedKetherParser("rotation") {
            it.group(double(), actionVector()).apply(it) { angle, vector ->
                now {
                    createIdentityMatrix().rotate(angle, vector)
                }
            }
        }

        @KetherEditor.Document("matrix of|preset rotate <angle:radian> <axis:x|y|z>")
        val rotationOn = KetherHelper.combinedKetherParser("rotation-on") {
            it.group(double(), text()).apply(it) { angle, axis ->
                now {
                    when (axis) {
                        "x" -> createIdentityMatrix().rotate(angle, 0)
                        "y" -> createIdentityMatrix().rotate(angle, 1)
                        "z" -> createIdentityMatrix().rotate(angle, 2)
                        else -> error("Unknown axis $axis")
                    }
                }
            }
        }

        @KetherEditor.Document("matrix of|preset scale <x:Number> <y:Number> <z:Number>")
        val scale = KetherHelper.combinedKetherParser("scale") {
            it.group(double(), double(), double()).apply(it) { x, y, z ->
                now {
                    createIdentityMatrix().scale(x, y, z)
                }
            }
        }
    }

    @KetherEditor.Document("matrix mult <matrix1:Matrix> <matrix2:Matrix>")
    val mult = KetherHelper.combinedKetherParser("mult") {
        it.group(actionTransformMatrix(), actionTransformMatrix()).apply(it) { a, b ->
            now {
                a!!.copy().mult(b)
            }
        }
    }

}