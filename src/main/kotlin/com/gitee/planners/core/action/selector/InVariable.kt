package com.gitee.planners.core.action.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.TargetBukkitLocation
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.core.action.getEnvironmentContext
import com.gitee.planners.core.action.getTargetContainer
import org.bukkit.Bukkit
import org.bukkit.Location
import taboolib.common.util.Vector
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestActionParser
import taboolib.library.kether.QuestContext
import taboolib.module.kether.combinationParser
import taboolib.module.kether.run
import taboolib.module.kether.str
import java.util.concurrent.CompletableFuture

// 内部选择器 不开放
class InVariable(val action: ParsedAction<*>) : QuestAction<Void>() {

    override fun process(frame: QuestContext.Frame): CompletableFuture<Void> {
        return frame.run(action).thenAccept { id ->
            val container = when (id) {
                is TargetContainer -> id
                is Vector -> {
                    // Use the world of the origin as the world of the location
                    val world = (frame.getEnvironmentContext().origin as TargetLocation<*>).getBukkitLocation().world
                    TargetContainer.of(TargetBukkitLocation(Location(world, id.x, id.y, id.z)))
                }
                is Location -> TargetContainer.of(TargetBukkitLocation(id))
                else -> frame.variables().get<TargetContainer>(id.toString()).orElse(TargetContainer())
            }
            frame.getTargetContainer() += container
        }
    }


}