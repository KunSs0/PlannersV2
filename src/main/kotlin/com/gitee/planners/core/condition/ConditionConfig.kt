package com.gitee.planners.core.condition

/**
 * 条件配置数据类。
 * 每个条件一条语义，对应一个 hint。
 */
data class ConditionConfig(
    /** 条件名（key） */
    val key: String,
    /** JS 校验表达式，返回 Boolean */
    val exper: String,
    /** 默认参数，值可为常量或 String 公式 */
    val props: Map<String, Any>,
    /** 校验失败提示，支持 {props.xxx} 变量插值 */
    val hint: String,
    /** JS 消耗语句，null 表示无消耗 */
    val consume: String?
)
