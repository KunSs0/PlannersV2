package com.gitee.planners.core.script.proxy

interface ProxyRouteTarget<P> {

    val proxySkillTrees: List<ProxyTreeDefinition>

    fun getNodeInfo(player: P, treeId: String, nodeId: String): ProxyNodeInfo
}
