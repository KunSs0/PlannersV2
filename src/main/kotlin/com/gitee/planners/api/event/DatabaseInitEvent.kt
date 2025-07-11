package com.gitee.planners.api.event

import com.gitee.planners.core.database.Database
import taboolib.platform.type.BukkitProxyEvent

class DatabaseInitEvent(val type:String, var instance: Database? = null): BukkitProxyEvent(){
    override val allowCancelled: Boolean = false
}