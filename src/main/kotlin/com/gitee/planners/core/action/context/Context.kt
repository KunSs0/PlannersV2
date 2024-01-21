package com.gitee.planners.core.action.context

import com.gitee.planners.api.job.target.Target

interface Context {

    // 执行者 不可变
    val sender: Target<*>

    // 原点 可变
    var origin: Target<*>

    fun process()

}
