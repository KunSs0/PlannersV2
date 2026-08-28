package com.gitee.planners.core.script.proxy

class ProxyPlayerRoute<P>(
    private val route: ProxyRouteTarget<P>,
    private val player: P
) {

    private class TreeIndex(val definition: ProxyTreeDefinition)

    private val treeIndexes = LinkedHashMap<String, TreeIndex>()
    private val nodeInfoCache = HashMap<String, ProxyNodeInfo>()

    init {
        for (tree in route.proxySkillTrees) {
            treeIndexes[tree.id] = TreeIndex(tree)
        }
    }

    @Synchronized
    private fun cachedNodeInfo(treeId: String, nodeId: String): ProxyNodeInfo {
        val key = "$treeId:$nodeId"
        val cached = nodeInfoCache[key]
        if (cached != null) {
            return cached
        }
        val result = route.getNodeInfo(player, treeId, nodeId)
        nodeInfoCache[key] = result
        return result
    }

    private fun view(treeId: String): TreeIndex {
        val result = treeIndexes[treeId]
        if (result == null) {
            throw IllegalArgumentException("Unknown skill tree: $treeId")
        }
        return result
    }

    /** @return 路线包含的技能树数量。 */
    fun getTreeCount(): Int {
        return route.proxySkillTrees.size
    }

    /** @return 指定索引的技能树 ID。 */
    fun getTreeId(index: Int): String {
        return route.proxySkillTrees[index].id
    }

    /** @return 指定技能树的节点数量。 */
    fun getNodeCount(treeId: String): Int {
        return view(treeId).definition.nodeIds.size
    }

    /** @return 指定技能树索引位置的节点 ID。 */
    fun getNodeIdAt(treeId: String, index: Int): String {
        return view(treeId).definition.nodeIds[index]
    }

    /** 一次反查指定节点的等级、升级结果和提示，同一代理内只计算一次。 */
    fun getNodeInfo(treeId: String, nodeId: String): ProxyNodeInfo {
        view(treeId)
        return cachedNodeInfo(treeId, nodeId)
    }
}
