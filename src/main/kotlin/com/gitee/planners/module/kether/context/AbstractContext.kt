package com.gitee.planners.module.kether.context

import com.gitee.planners.api.job.target.Target

abstract class AbstractContext(final override val sender: Target<*>) : Context {

    override var origin: Target<*> = sender

}
