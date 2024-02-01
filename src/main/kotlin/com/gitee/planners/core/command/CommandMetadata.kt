package com.gitee.planners.core.command

import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.util.math.scale
import org.ejml.simple.SimpleMatrix

object CommandMetadata {

    val clear = with { player ->
        player.adaptTarget()
    }

}
