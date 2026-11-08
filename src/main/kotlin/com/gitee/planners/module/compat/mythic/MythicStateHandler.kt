package com.gitee.planners.module.compat.mythic

import com.gitee.planners.module.compat.mythic.mm5.MythicDamageMechanic
import com.gitee.planners.module.compat.mythic.mm5.MythicStateAttachMechanic
import com.gitee.planners.module.compat.mythic.mm5.MythicStateCustomTriggerMechanic
import com.gitee.planners.module.compat.mythic.mm5.MythicStateDetachMechanic
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent as MythicMechanicLoadEvent5
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent as MythicMechanicLoadEvent4
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerBukkitListener
import java.util.*

/**
 * 注册 Planners 提供的 MythicMobs mechanic。
 *
 * MythicMobs 4 与 5 的 API 完全分离，因此按主版本号分别注册；
 * 两侧 mechanic 名称与参数保持一致。
 */
object MythicStateHandler {

    @Awake(LifeCycle.ENABLE)
    fun init() {
        if (!MythicMobsLoader.isEnable) {
            return
        }

        when (MythicMobsLoader.version.getOrNull(0)) {
            4 -> registerMythicMobs4()
            5 -> registerMythicMobs5()
            else -> info(
                "MythicStateHandler: MythicMobs ${MythicMobsLoader.version.joinToString(".")} is not supported for state mechanic registration yet."
            )
        }
    }

    private fun registerMythicMobs4() {
        info("MythicStateHandler: registering MythicMobs v4 state mechanic support.")
        registerBukkitListener(MythicMechanicLoadEvent4::class.java) { event ->
            when (event.mechanicName.lowercase(Locale.ROOT)) {
                "plstateattach", "pl-state-attach" ->
                    event.register(com.gitee.planners.module.compat.mythic.MythicStateAttachMechanic(event.config))
                "plstatedetach", "pl-state-detach" ->
                    event.register(com.gitee.planners.module.compat.mythic.MythicStateDetachMechanic(event.config))
                "plstatecustomtrigger", "pl-state-customtrigger" ->
                    event.register(com.gitee.planners.module.compat.mythic.MythicStateCustomTriggerMechanic(event.config))
                "pldamage", "pl-damage" ->
                    event.register(com.gitee.planners.module.compat.mythic.MythicDamageMechanic(event.config))
            }
        }
    }

    private fun registerMythicMobs5() {
        info("MythicStateHandler: registering MythicMobs v5 state mechanic support.")
        registerBukkitListener(MythicMechanicLoadEvent5::class.java) { event ->
            val container = event.container
            val line = container.configLine ?: event.mechanicName
            when (event.mechanicName.lowercase(Locale.ROOT)) {
                "plstateattach", "pl-state-attach" ->
                    event.register(MythicStateAttachMechanic(container, line, event.config))
                "plstatedetach", "pl-state-detach" ->
                    event.register(MythicStateDetachMechanic(container, line, event.config))
                "plstatecustomtrigger", "pl-state-customtrigger" ->
                    event.register(MythicStateCustomTriggerMechanic(container, line, event.config))
                "pldamage", "pl-damage" ->
                    event.register(MythicDamageMechanic(container, line, event.config))
            }
        }
    }
}
