package com.gitee.planners.api.directing

/**
 * 指向性选择确认结果。
 *
 * 具体 provider 使用子类型承载实体、位置或方向等结果。Planners 将该对象注入技能执行上下文，
 * 不将其转换为既有 target 语义。
 */
interface DirectingResult {

    /**
     * 产生此结果的 provider 类型。
     */
    val type: String
}
