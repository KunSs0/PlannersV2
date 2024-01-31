package com.gitee.planners.module.kether.bukkit

import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.target.*
import com.gitee.planners.module.kether.enum
import com.gitee.planners.module.kether.commandObjective
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.expansion.fakeOp
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

@CombinationKetherParser.Used
object ActionBukkitCommand : SimpleKetherParser("command") {

    override fun run(): QuestActionParser {
        return combinationParser {
            it.group(
                text(),
                command("as", then = enum<SenderType>()).option().defaultsTo(SenderType.PLAYER),
                commandObjective(type = LeastType.SENDER)
            ).apply(it) { command, senderType, objective ->
                now {
                    objective.filterIsInstance<TargetCommandSender<*>>().forEach { senderType!!.dispatchCommand(it, command) }
                }
            }
        }
    }

    enum class SenderType {

        OP {
            override fun dispatchCommand(target: TargetCommandSender<*>, command: String) {
                if (target is TargetBukkitEntity) {
                    val player = (target.getInstance() as? Player)?.fakeOp() ?: return
                    player.adaptTarget().dispatchCommand(command)
                } else if (target is TargetConsoleCommandSender) {
                    target.dispatchCommand(command)
                }
            }
        },
        PLAYER {
            override fun dispatchCommand(target: TargetCommandSender<*>, command: String) {
                if (target is TargetBukkitEntity) {
                    target.dispatchCommand(command)
                } else if (target is TargetConsoleCommandSender) {
                    CONSOLE.dispatchCommand(target, command)
                }
            }
        },
        CONSOLE {
            override fun dispatchCommand(target: TargetCommandSender<*>, command: String) {
                TargetConsoleCommandSender(Bukkit.getConsoleSender()).dispatchCommand(command)
            }
        };

        abstract fun dispatchCommand(target: TargetCommandSender<*>, command: String)

    }

}
