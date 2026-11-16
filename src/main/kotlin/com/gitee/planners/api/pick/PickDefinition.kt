package com.gitee.planners.api.pick

/**
 * 指向性技能的不可变定义。
 *
 * 定义由 Planners 在技能加载阶段解析并保存在技能对象中；`position`、`entity`、`direction`
 * 都是内置类型。客户端表现由 [PickPresentation] 实现，服务端校验由 Planners 自己完成。
 */
interface PickDefinition {

    /**
     * 当前定义的类型标识。
     */
    val type: String

    /**
     * 指向会话允许保持的最长 tick 数。
     */
    val maxDurationTicks: Int

    /**
     * 该类型对表现层公开的字段。
     *
     * 表现后端直接把这些字段透传给客户端，因此脚本不需要了解具体子类。
     *
     * @return 字段名到取值的映射。
     */
    fun fields(): Map<String, Any?>
}
