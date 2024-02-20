package com.gitee.planners.module.kether.compat.vault

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.kether.commandObjectiveOrSender
import org.bukkit.entity.Player
import taboolib.common.util.unsafeLazy
import taboolib.platform.compat.VaultService

@KetherEditor.Document("balance ...")
@CombinationKetherParser.Used
object ActionBalance : MultipleKetherParser("balance") {

    private val hooked by unsafeLazy { VaultService.economy!! }

    @KetherEditor.Document("balance has <value:Number> [at objective:TargetContainer(sender)]")
    val has = KetherHelper.combinedKetherParser("has") {
        it.group(
            double(),
            commandObjectiveOrSender()
        ).apply(it) { value, objective ->
            now {
                val player = objective.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.getInstance() as? Player
                    ?: error("sender must be a player")
                hooked.has(player, value)
            }
        }
    }

    @KetherEditor.Document("balance get [at objective:TargetContainer(sender)]")
    val get = KetherHelper.combinedKetherParser("get") {
        it.group(
            commandObjectiveOrSender()
        ).apply(it) { objective ->
            now {
                val player = objective.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.getInstance() as? Player
                    ?: error("sender must be a player")
                hooked.getBalance(player)
            }
        }
    }

    @KetherEditor.Document("balance give <value:Number> [at objective:TargetContainer(sender)]")
    val deposit = KetherHelper.combinedKetherParser("give") {
        it.group(
            double(),
            commandObjectiveOrSender()
        ).apply(it) { value, objective ->
            now {
                val player = objective.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.getInstance() as? Player
                    ?: error("sender must be a player")
                hooked.depositPlayer(player, value)
            }
        }
    }

    @KetherEditor.Document("balance take <value:Number> [at objective:TargetContainer(sender)]")
    val withdraw = KetherHelper.combinedKetherParser("take") {
        it.group(
            double(),
            commandObjectiveOrSender()
        ).apply(it) { value, objective ->
            now {
                val player = objective.filterIsInstance<TargetBukkitEntity>().firstOrNull()?.getInstance() as? Player
                    ?: error("sender must be a player")
                hooked.withdrawPlayer(player, value)
            }
        }
    }

}