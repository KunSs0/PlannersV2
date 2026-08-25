package com.gitee.planners.core.player

import com.gitee.planners.core.config.ImmutableSkillTree
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

/**
 * 逐节点反查目标：代理视图所需的最小业务查询集。
 *
 * 生产侧由 [PlayerRoute] 实现；测试侧的 Mock 路由实现同一接口，
 * 保证脚本访问的是同一套生产代理逻辑。
 */
interface RealLookupTarget {

    /** 按配置顺序排列的职业阶段技能树定义。 */
    val lookupSkillTrees: List<RealLookupTreeDefinition>

    fun getNodeLevel(treeId: String, nodeId: String): Int

    fun isNodeCanAdvance(treeId: String, nodeId: String): Boolean

    fun getNodeHints(treeId: String, nodeId: String): List<String>
}

/** 单棵树的静态定义（节点 ID 按配置顺序排列）。 */
interface RealLookupTreeDefinition {

    val id: String

    val nodeIds: List<String>
}

/**
 * PlayerRoute 的 polyglot 业务代理视图。
 *
 * 保持"JS 循环逐节点反查"的真实业务语义：每个成员都是一次独立的业务方法调用，
 * 不做任何批量合并返回。相对反射式宿主对象，方法分派走预构建的 ProxyExecutable，
 * 树/节点索引 O(1)，显著降低跨语言分派成本。
 */
class PlayerRoutePolyglotView(
    private val route: RealLookupTarget,
    private val canAdvanceCacheEnabled: Boolean = true
) : ProxyObject {

    private class TreeIndex(val definition: RealLookupTreeDefinition) {
        val indexByNodeId: Map<String, Int> = buildMap {
            definition.nodeIds.forEachIndexed { index, nodeId -> put(nodeId, index) }
        }
    }

    private val treeIndexes: Map<String, TreeIndex> = buildMap {
        for (tree in route.lookupSkillTrees) {
            put(tree.id, TreeIndex(tree))
        }
    }

    private val canAdvanceCache = HashMap<String, Boolean>()
    private val hintsCache = HashMap<String, List<String>>()

    private val members: Map<String, ProxyExecutable> = mapOf(
        "getTreeCount" to exec { _ -> route.lookupSkillTrees.size },
        "getTreeId" to exec { args -> route.lookupSkillTrees[args[0].asInt()].id },
        "getNodeCount" to exec { args ->
            view(args[0].asString()).definition.nodeIds.size
        },
        "getNodeIdAt" to exec { args ->
            view(args[0].asString()).definition.nodeIds[args[1].asInt()]
        },
        "getNodeLevelById" to exec { args ->
            val treeId = args[0].asString()
            val nodeId = args[1].asString()
            route.getNodeLevel(treeId, nodeId)
        },
        "isNodeCanAdvance" to exec { args ->
            val treeId = args[0].asString()
            val nodeId = args[1].asString()
            if (canAdvanceCacheEnabled) {
                cachedCanAdvance(treeId, nodeId)
            } else {
                route.isNodeCanAdvance(treeId, nodeId)
            }
        },
        "getNodeHints" to exec { args ->
            val treeId = args[0].asString()
            val nodeId = args[1].asString()
            StringListProxy(cachedHints(treeId, nodeId))
        }
    )

    /** 同一请求内节点状态不变，缓存反查结果避免重复业务计算。 */
    @Synchronized
    private fun cachedCanAdvance(treeId: String, nodeId: String): Boolean {
        val key = "$treeId:$nodeId"
        return canAdvanceCache.getOrPut(key) { route.isNodeCanAdvance(treeId, nodeId) }
    }

    @Synchronized
    private fun cachedHints(treeId: String, nodeId: String): List<String> {
        val key = "$treeId:$nodeId"
        return hintsCache.getOrPut(key) { route.getNodeHints(treeId, nodeId) }
    }

    private fun view(treeId: String): TreeIndex {
        return treeIndexes[treeId] ?: throw IllegalArgumentException("未知技能树: $treeId")
    }

    private fun exec(block: (Array<Value>) -> Any?): ProxyExecutable {
        return ProxyExecutable { arguments -> block(arguments) }
    }

    override fun getMember(key: String?): Any {
        return members[key] ?: throw IllegalArgumentException("未知成员: $key")
    }

    override fun getMemberKeys(): Any {
        return members.keys
    }

    override fun hasMember(key: String?): Boolean {
        return members.containsKey(key)
    }

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException("只读")
    }
}

/** 只读字符串列表的 polyglot 数组代理：JS 侧按原生数组索引直读。 */
class StringListProxy(private val data: List<String>) : ProxyArray {

    override fun get(index: Long): Any {
        return data[index.toInt()]
    }

    override fun set(index: Long, value: Value?) {
        throw UnsupportedOperationException("只读")
    }

    override fun remove(index: Long): Boolean {
        throw UnsupportedOperationException("只读")
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }
}

/** [ImmutableSkillTree] 的最小定义视图，供代理索引构建使用。 */
class TreeDefinitionView(tree: ImmutableSkillTree) : RealLookupTreeDefinition {

    override val id: String = tree.id

    override val nodeIds: List<String> = tree.nodes.keys.toList()
}
