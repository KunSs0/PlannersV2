package com.gitee.planners.module.kether.context

import com.gitee.planners.api.job.target.Target
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

interface Context : Callable<CompletableFuture<Any>> {

    // 执行者 不可变
    val sender: Target<*>

    // 原点 可变
    var origin: Target<*>


}
