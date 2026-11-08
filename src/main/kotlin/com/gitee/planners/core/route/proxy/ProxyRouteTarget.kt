package com.gitee.planners.core.route.proxy

interface ProxyRouteTarget<P> {

    val proxySkillTrees: List<ProxyTreeDefinition>

    fun getNodeInfo(player: P, treeId: String, nodeId: String): ProxyNodeInfo
}
