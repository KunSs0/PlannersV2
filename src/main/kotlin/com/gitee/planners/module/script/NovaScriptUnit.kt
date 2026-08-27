package com.gitee.planners.module.script

/**
 * 一段已经登记到 Planners Workspace 的 Nova 源码描述。
 *
 * @property moduleId Workspace 内唯一的模块入口。
 * @property functionName 业务调用的导出函数名。
 */
class NovaScriptUnit(
    val moduleId: String,
    val functionName: String
)
