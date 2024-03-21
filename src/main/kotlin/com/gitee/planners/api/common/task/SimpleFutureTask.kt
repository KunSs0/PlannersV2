package com.gitee.planners.api.common.task

import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync

open class SimpleFutureTask(val tick: Long, async: Boolean = false, val onClose: Runnable) {

    private val task = submit(async = async, delay = tick) {
        this@SimpleFutureTask.handleClose()
    }

    protected open fun handleClose() {
        onClose.run()
    }

}
