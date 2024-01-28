package com.gitee.planners.api.event.action

import com.gitee.planners.module.binding.Combined
import com.gitee.planners.module.binding.CombinedAnalyzer
import com.gitee.planners.module.binding.InteractionAction
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

abstract class CombinedEvent(val player: Player, val combined: Combined) : BukkitProxyEvent() {

    class PressIn(player: Player, val inference: CombinedAnalyzer.Inference, val action: InteractionAction) : CombinedEvent(player, inference.combined)

    class Begin(player: Player, combined: Combined) : CombinedEvent(player, combined)

    class Close(player: Player, val inference: CombinedAnalyzer.Inference) : CombinedEvent(player, inference.combined)

}
