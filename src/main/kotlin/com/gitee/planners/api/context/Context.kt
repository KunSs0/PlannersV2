package com.gitee.planners.api.context

import com.gitee.planners.api.job.target.ProxyTarget

/**
 * 脚本上下文接口
 */
interface Context {

    /** 执行者 (不可变) */
    val sender: ProxyTarget<*>?

    /** 原点 (可变) */
    var origin: ProxyTarget<*>?
}
