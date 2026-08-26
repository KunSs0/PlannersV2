package com.gitee.planners.core.script.proxy

interface ProxyRouteTarget<P> {

    val proxySkillTrees: List<ProxyTreeDefinition>

    fun getNodeLevel(treeId: String, nodeId: String): Int

    fun isNodeCanAdvance(player: P, treeId: String, nodeId: String): Boolean

    fun getNodeHints(player: P, treeId: String, nodeId: String): List<String>
}
