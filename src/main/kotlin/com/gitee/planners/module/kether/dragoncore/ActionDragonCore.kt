package com.gitee.planners.module.kether.dragoncore

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.commandObjective

object ActionDragonCore : MultipleKetherParser("dragoncore") {

    val animation = object : MultipleKetherParser("animation") {

        @KetherEditor.Document("dragoncore animation send [name: string] [transition: 0] [selector: TargetContainer(sender)]")
        val send = KetherHelper.combinedKetherParser {
            it.group(text(), int(), commandObjective()).apply(it) { name, transition, objective ->
                now {
                    objective.filterIsInstance<TargetBukkitEntity>().forEach {
                        it.instance.sendAnimation(name, transition)
                    }
                }
            }
        }

    }


}
