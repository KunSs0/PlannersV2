package com.gitee.planners.core.command

import com.gitee.planners.api.job.target.adaptTarget

object CommandMetadata {

    val clear = with { player ->
        player.adaptTarget()
    }

}
