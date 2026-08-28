package com.gitee.planners.core.script.proxy

/** 单个技能树节点面向脚本的不可变查询结果。 */
class ProxyNodeInfo(
    val treeId: String,
    val nodeId: String,
    val level: Int,
    val canAdvance: Boolean,
    val hints: List<String>
)
