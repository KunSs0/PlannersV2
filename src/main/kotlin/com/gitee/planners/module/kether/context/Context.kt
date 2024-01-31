package com.gitee.planners.module.kether.context

import com.gitee.planners.api.job.target.Target

interface Context : Runnable{

    // 执行者 不可变
    val sender: Target<*>

    // 原点 可变
    var origin: Target<*>


}
