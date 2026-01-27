package com.gitee.planners.core.command

import com.gitee.planners.api.job.target.asTarget

object CommandMetadata {

    val clear = with { player ->
        player.asTarget()
    }

}
