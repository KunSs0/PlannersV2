package com.gitee.planners.api.directing

/**
 * 指向性技能的不可变定义。
 *
 * 定义由 [DirectingProvider] 在技能加载阶段解析，并保存在技能对象中。
 * Planners 只读取持续时间；具体的选中参数、客户端表现和服务端校验均由 provider 持有。
 */
interface DirectingDefinition {

    /**
     * 当前定义所属的 provider 类型。
     */
    val type: String

    /**
     * 指向会话允许保持的最长 tick 数。
     */
    val maxDurationTicks: Int
}
