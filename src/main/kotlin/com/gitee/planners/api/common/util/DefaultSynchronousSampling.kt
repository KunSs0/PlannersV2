package com.gitee.planners.api.common.util

import taboolib.common.util.runSync

class DefaultSynchronousSampling<T>(val block: () -> T): SynchronousSampling<T> {

    override fun get(): T {
        return runSync(block)
    }

}
