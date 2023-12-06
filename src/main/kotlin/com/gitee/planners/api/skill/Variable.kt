package com.gitee.planners.api.skill

import com.gitee.planners.api.script.KetherScript
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptCommandSender
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptOptions
import java.util.concurrent.CompletableFuture

interface Variable : KetherScript {

    val id: String

}

