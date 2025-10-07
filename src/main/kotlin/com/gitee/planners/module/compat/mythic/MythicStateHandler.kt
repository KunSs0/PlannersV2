package com.gitee.planners.module.compat.mythic

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info

object MythicStateHandler {

    @Awake(LifeCycle.ENABLE)
    fun init() {
        if (!MythicMobsLoader.isEnable) {
            return
        }

        when (MythicMobsLoader.version.getOrNull(0)) {
            4 -> registerMythicMobs4()
            else -> info("MythicStateHandler: MythicMobs ${MythicMobsLoader.version.joinToString(".")} is not supported for state mechanic registration yet.")
        }
    }

    private fun registerMythicMobs4() {
        info("MythicStateHandler: registering MythicMobs v4 state mechanic support.")
//        registerBukkitListener(MythicMechanicLoadEvent::class.java) { event ->
//            when (event.mechanicName.lowercase(Locale.ROOT)) {
//                "plstateattach", "pl-state-attach" -> event.register(MythicStateAttachMechanic(event.config))
//                "plstatedetach", "pl-state-detach" -> event.register(MythicStateDetachMechanic(event.config))
//            }
//        }
    }
}
