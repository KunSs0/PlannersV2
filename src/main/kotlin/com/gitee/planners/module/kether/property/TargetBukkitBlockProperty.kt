package com.gitee.planners.module.kether.property

import com.gitee.planners.api.job.target.TargetBlock
import taboolib.common.OpenResult
import taboolib.common.platform.function.warning
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty


@KetherProperty(TargetBlock::class)
fun blockProperty() = object : ScriptProperty<TargetBlock>("operator.bukkit-block") {

    override fun read(instance: TargetBlock, key: String): OpenResult {
        return when (key) {

            "x" -> OpenResult.successful(instance.instance.x)

            "y" -> OpenResult.successful(instance.instance.y)

            "z" -> OpenResult.successful(instance.instance.z)

            "world" -> OpenResult.successful(instance.instance.world)
            else -> {
                warning("Unknown key: $key")
                OpenResult.failed()
            }
        }
    }

    override fun write(instance: TargetBlock, key: String, value: Any?): OpenResult {
        TODO("Not yet implemented")
    }
}
