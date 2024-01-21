package com.gitee.planners.core.action.context

import com.gitee.planners.api.job.target.Target

abstract class AbstractContext(final override val sender: Target<*>) : Context {

    override var origin: Target<*> = sender

}
