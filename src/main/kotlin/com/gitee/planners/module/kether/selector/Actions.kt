package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import org.bukkit.Bukkit


private object Sender : AbstractSelector("sender","self") {

    override fun select(): SimpleKetherParser {
        return KetherHelper.simpleKetherVoid {
            getTargetContainer() += getEnvironmentContext().sender
        }
    }
}

private object Their : AbstractSelector("their") {

    override fun select(): SimpleKetherParser {
        return KetherHelper.simpleKetherVoid {
            val senderId = (getEnvironmentContext().sender as? TargetEntity)?.getUniqueId() ?:
                TODO("Non TargetEntity sender is currently not supported in @their selector.")

            getTargetContainer().removeIf {
                it is TargetEntity && it.getUniqueId() == senderId
            }
        }
    }
}

private object Console : AbstractSelector("console") {

    override fun select(): SimpleKetherParser {
        return KetherHelper.simpleKetherVoid {
            getTargetContainer() += Bukkit.getConsoleSender().adaptTarget()
        }
    }

}

@KetherEditor.Document(value = "select <objective...>", result = TargetContainer::class)
@CombinationKetherParser.Used
private fun actionSelect() = KetherHelper.simpleKetherParser("select") {
    TargetContainerParser.parser(emptyArray(),it,LeastType.EMPTY)
}



