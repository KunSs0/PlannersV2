package com.gitee.planners.core.script.proxy

import com.gitee.scriptengine.api.ScriptFunction
import com.gitee.scriptengine.api.ScriptObject
import com.gitee.scriptengine.api.ScriptValue

class ProxyPlayerRoute<P>(
    private val route: ProxyRouteTarget<P>,
    private val player: P
) : ScriptObject {

    private class TreeIndex(val definition: ProxyTreeDefinition)

    private val treeIndexes = LinkedHashMap<String, TreeIndex>()
    private val canAdvanceCache = HashMap<String, Boolean>()
    private val hintsCache = HashMap<String, List<String>>()
    private val members = LinkedHashMap<String, ScriptFunction>()

    init {
        for (tree in route.proxySkillTrees) {
            treeIndexes[tree.id] = TreeIndex(tree)
        }
        members["getTreeCount"] = function { route.proxySkillTrees.size }
        members["getTreeId"] = function { arguments ->
            route.proxySkillTrees[arguments[0].asInt()].id
        }
        members["getNodeCount"] = function { arguments ->
            view(arguments[0].asString()).definition.nodeIds.size
        }
        members["getNodeIdAt"] = function { arguments ->
            view(arguments[0].asString()).definition.nodeIds[arguments[1].asInt()]
        }
        members["getNodeLevel"] = function { arguments ->
            route.getNodeLevel(arguments[0].asString(), arguments[1].asString())
        }
        members["canAdvanceNode"] = function { arguments ->
            cachedCanAdvance(arguments[0].asString(), arguments[1].asString())
        }
        members["getNodeHints"] = function { arguments ->
            ProxyStringList(cachedHints(arguments[0].asString(), arguments[1].asString()))
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
            throw IllegalArgumentException("未知技能树: $treeId")
        }
        return result
    }

    private fun function(block: (Array<out ScriptValue>) -> Any?): ScriptFunction {
        return ScriptFunction { arguments -> block(arguments) }
    }

    override fun getMember(key: String): Any? {
        val result = members[key]
        if (result == null) {
            throw IllegalArgumentException("未知成员: $key")
        }
        return result
    }

    override fun getMemberKeys(): Array<String> {
        return members.keys.toTypedArray()
    }

    override fun hasMember(key: String): Boolean {
        return members.containsKey(key)
    }

    override fun putMember(key: String, value: ScriptValue) {
        throw UnsupportedOperationException("只读")
    }
}
