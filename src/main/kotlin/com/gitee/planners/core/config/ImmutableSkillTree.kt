package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection

enum class TreeType {
    /** 基础流派（初始可选） */
    BASE,
    /** 分支流派（转职后解锁） */
    BRANCH
}

/**
 * 职业技能树定义。
 * 一个技能树对应一个职业流派，包含技能节点集合和拓扑关系图。
 */
class ImmutableSkillTree(
    val id: String,
    val name: String,
    val clazz: String,
    val type: TreeType,
    /** skillId → SkillNode */
    val nodes: Map<String, SkillNode>,
    /** nodeId → [前置 nodeId]，纯拓扑 */
    val graph: Map<String, List<String>>
) {
    companion object {
        fun parse(key: String, config: ConfigurationSection): ImmutableSkillTree {
            val name = config.getString("name") ?: key
            val clazz = config.getString("class") ?: "none"
            val type = try {
                TreeType.valueOf(config.getString("type", "base")!!.uppercase())
            } catch (e: Exception) {
                TreeType.BASE
            }

            // nodes
            val nodesSection = config.getConfigurationSection("nodes")
                ?: throw IllegalArgumentException("SkillTree '$key' 缺少 nodes 节点")
            val nodes = mutableMapOf<String, SkillNode>()
            for (nodeKey in nodesSection.getKeys(false)) {
                val nodeSection = nodesSection.getConfigurationSection(nodeKey) ?: continue
                // 校验 node key 对应 skill 存在（警告但不阻塞）
                if (Registries.SKILL.getOrNull(nodeKey) == null) {
                    warning("SkillTree '$key': node '$nodeKey' 对应的 ImmutableSkill 尚未注册")
                }
                nodes[nodeKey] = SkillNode.parse(nodeSection)
            }

            // graph
            val graphSection = config.getConfigurationSection("graph")
            val graph = mutableMapOf<String, List<String>>()
            if (graphSection != null) {
                for (nodeId in graphSection.getKeys(false)) {
                    val prereqs = graphSection.getStringList(nodeId)
                    // 校验 graph 中的节点都在 nodes 中
                    if (!nodes.containsKey(nodeId)) {
                        throw IllegalArgumentException(
                            "SkillTree '$key': graph 中的节点 '$nodeId' 不在 nodes 中"
                        )
                    }
                    prereqs.forEach { prereq ->
                        if (!nodes.containsKey(prereq)) {
                            throw IllegalArgumentException(
                                "SkillTree '$key': graph 中 '$nodeId' 的前置节点 '$prereq' 不在 nodes 中"
                            )
                        }
                    }
                    graph[nodeId] = prereqs
                }
            }

            return ImmutableSkillTree(key, name, clazz, type, nodes, graph)
        }
    }
}
