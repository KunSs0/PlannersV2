package com.gitee.planners.core.script.proxy

class ProxyPlayerRoute<P>(
    private val route: ProxyRouteTarget<P>,
    private val player: P
) {

    private class TreeIndex(val definition: ProxyTreeDefinition)

    private val treeIndexes = LinkedHashMap<String, TreeIndex>()
    private val canAdvanceCache = HashMap<String, Boolean>()
    private val hintsCache = HashMap<String, List<String>>()

    init {
        for (tree in route.proxySkillTrees) {
            treeIndexes[tree.id] = TreeIndex(tree)
        }
    }

    @Synchronized
    private fun cachedCanAdvance(treeId: String, nodeId: String): Boolean {
        val key = "$treeId:$nodeId"
        val cached = canAdvanceCache[key]
        if (cached != null) {
            return cached
        }
        val result = route.isNodeCanAdvance(player, treeId, nodeId)
        canAdvanceCache[key] = result
        return result
    }

    @Synchronized
    private fun cachedHints(treeId: String, nodeId: String): List<String> {
        val key = "$treeId:$nodeId"
        val cached = hintsCache[key]
        if (cached != null) {
            return cached
        }
        val result = route.getNodeHints(player, treeId, nodeId)
        hintsCache[key] = result
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

    /** @return 指定节点的当前等级。 */
    fun getNodeLevel(treeId: String, nodeId: String): Int {
        return route.getNodeLevel(treeId, nodeId)
    }

    /** @return 指定节点当前能否升级。 */
    fun canAdvanceNode(treeId: String, nodeId: String): Boolean {
        return cachedCanAdvance(treeId, nodeId)
    }

    /** @return 指定节点当前的提示文本。 */
    fun getNodeHints(treeId: String, nodeId: String): ProxyStringList {
        return ProxyStringList(cachedHints(treeId, nodeId))
    }
}
