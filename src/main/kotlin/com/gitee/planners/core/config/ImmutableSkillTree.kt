package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import taboolib.library.configuration.ConfigurationSection

class ImmutableSkillTree(
    val id: String,
    val name: String,
    val clazz: String,
    val type: TreeType,
    val nodes: Map<String, SkillTreeNode>,
    val graph: Map<String, List<SkillTreeNodeRequirement>>
) {

    fun getActiveAndAttributeNodes(): List<SkillTreeNode> {
        val result = ArrayList<SkillTreeNode>()
        for (node in nodes.values) {
            if (!isPassiveSkillNode(node)) {
                result.add(node)
            }
        }
        return result
    }

    fun getPassiveSkillNodes(): List<SkillTreeNode> {
        val result = ArrayList<SkillTreeNode>()
        for (node in nodes.values) {
            if (isPassiveSkillNode(node)) {
                result.add(node)
            }
        }
        return result
    }

    private fun isPassiveSkillNode(node: SkillTreeNode): Boolean {
        if (node !is SkillTreeSkillNode) {
            return false
        }
        val skill = Registries.SKILL.getOrNull(node.skillId)
        if (skill == null) {
            error("SkillTree '$id' 的节点 '${node.id}' 引用了不存在的技能 '${node.skillId}'")
        }
        return skill.categories.contains("passive")
    }

    companion object {

        fun parse(key: String, config: ConfigurationSection): ImmutableSkillTree {
            val name = config.getString("name") ?: key
            val clazz = config.getString("class") ?: "none"
            val type = parseTreeType(config)
            val nodes = parseNodes(key, config)
            val graph = parseGraph(key, config, nodes)
            validateNodeLevels(key, nodes)
            return ImmutableSkillTree(key, name, clazz, type, nodes, graph)
        }

        private fun parseTreeType(config: ConfigurationSection): TreeType {
            val rawType = config.getString("type", "base") ?: "base"
            return try {
                TreeType.valueOf(rawType.uppercase())
            } catch (exception: IllegalArgumentException) {
                error("SkillTree '${config.name}' 的 type 无效: $rawType")
            }
        }

        private fun parseNodes(treeId: String, config: ConfigurationSection): Map<String, SkillTreeNode> {
            val section = config.getConfigurationSection("nodes")
            if (section == null) {
                error("SkillTree '$treeId' 缺少 nodes 节点")
            }
            val nodes = LinkedHashMap<String, SkillTreeNode>()
            for (nodeId in section.getKeys(false)) {
                val nodeSection = section.getConfigurationSection(nodeId)
                if (nodeSection == null) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 不是配置节")
                }
                nodes[nodeId] = parseNode(treeId, nodeId, nodeSection)
            }
            return nodes
        }

        private fun parseNode(treeId: String, nodeId: String, config: ConfigurationSection): SkillTreeNode {
            val rawType = config.getString("type")
            if (rawType == null) {
                error("SkillTree '$treeId' 的节点 '$nodeId' 缺少 type")
            }
            val type = try {
                SkillTreeNodeType.valueOf(rawType.uppercase())
            } catch (exception: IllegalArgumentException) {
                error("SkillTree '$treeId' 的节点 '$nodeId' type 无效: $rawType")
            }
            val maxLevel = config.getInt("maxLevel", 1)
            if (maxLevel <= 0) {
                error("SkillTree '$treeId' 的节点 '$nodeId' maxLevel 必须大于 0")
            }
            val position = parsePosition(treeId, nodeId, config)
            val levels = SkillNode.parseLevels(config)
            return when (type) {
                SkillTreeNodeType.SKILL -> parseSkillNode(treeId, nodeId, config, maxLevel, levels, position)
                SkillTreeNodeType.ATTRIBUTE -> parseAttributeNode(treeId, nodeId, config, maxLevel, levels, position)
            }
        }

        private fun parsePosition(treeId: String, nodeId: String, config: ConfigurationSection): SkillTreeNodePosition {
            val section = config.getConfigurationSection("position")
            if (section == null) {
                error("SkillTree '$treeId' 的节点 '$nodeId' 缺少 position")
            }
            return SkillTreeNodePosition(section.getInt("x"), section.getInt("y"))
        }

        private fun parseSkillNode(
            treeId: String,
            nodeId: String,
            config: ConfigurationSection,
            maxLevel: Int,
            levels: Map<Int, Map<String, Map<String, Any>>>,
            position: SkillTreeNodePosition
        ): SkillTreeSkillNode {
            val skillId = config.getString("skill")
            if (skillId.isNullOrBlank()) {
                error("SkillTree '$treeId' 的技能节点 '$nodeId' 缺少 skill")
            }
            if (Registries.SKILL.getOrNull(skillId) == null) {
                error("SkillTree '$treeId' 的技能节点 '$nodeId' 引用了不存在的技能 '$skillId'")
            }
            return SkillTreeSkillNode(nodeId, skillId, maxLevel, levels, position)
        }

        private fun parseAttributeNode(
            treeId: String,
            nodeId: String,
            config: ConfigurationSection,
            maxLevel: Int,
            levels: Map<Int, Map<String, Map<String, Any>>>,
            position: SkillTreeNodePosition
        ): SkillTreeAttributeNode {
            val providerSection = config.getConfigurationSection("provider")
            if (providerSection == null) {
                error("SkillTree '$treeId' 的属性节点 '$nodeId' 缺少 provider")
            }
            val providerId = providerSection.getString("id")
            if (providerId.isNullOrBlank()) {
                error("SkillTree '$treeId' 的属性节点 '$nodeId' 缺少 provider.id")
            }
            val valuesSection = providerSection.getConfigurationSection("values")
            if (valuesSection == null || valuesSection.getKeys(false).isEmpty()) {
                error("SkillTree '$treeId' 的属性节点 '$nodeId' 缺少 provider.values")
            }
            val values = LinkedHashMap<String, Double>()
            for ((attributeId, rawValue) in valuesSection.getValues(false)) {
                val value = rawValue.toString().toDoubleOrNull()
                if (value == null) {
                    error("SkillTree '$treeId' 的属性节点 '$nodeId' provider.values.$attributeId 不是数值")
                }
                values[attributeId] = value
            }
            return SkillTreeAttributeNode(nodeId, providerId, values, maxLevel, levels, position)
        }

        private fun parseGraph(
            treeId: String,
            config: ConfigurationSection,
            nodes: Map<String, SkillTreeNode>
        ): Map<String, List<SkillTreeNodeRequirement>> {
            val section = config.getConfigurationSection("graph")
            if (section == null) {
                error("SkillTree '$treeId' 缺少 graph 节点")
            }
            val graph = LinkedHashMap<String, List<SkillTreeNodeRequirement>>()
            for (nodeId in nodes.keys) {
                if (!section.contains(nodeId)) {
                    error("SkillTree '$treeId' 的 graph 缺少节点 '$nodeId'")
                }
            }
            for (nodeId in section.getKeys(false)) {
                if (!nodes.containsKey(nodeId)) {
                    error("SkillTree '$treeId' 的 graph 包含未知节点 '$nodeId'")
                }
                graph[nodeId] = parseRequirements(treeId, nodeId, section, nodes)
            }
            validateAcyclic(treeId, graph)
            return graph
        }

        private fun parseRequirements(
            treeId: String,
            nodeId: String,
            graphSection: ConfigurationSection,
            nodes: Map<String, SkillTreeNode>
        ): List<SkillTreeNodeRequirement> {
            val rawRequirements = graphSection.getList(nodeId) ?: emptyList<Any>()
            val requirements = ArrayList<SkillTreeNodeRequirement>()
            for (rawRequirement in rawRequirements) {
                if (rawRequirement == null) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 包含空前置定义")
                }
                val requirement = parseRequirement(treeId, nodeId, rawRequirement)
                val target = nodes[requirement.nodeId]
                if (target == null) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 引用了未知前置 '${requirement.nodeId}'")
                }
                if (requirement.minLevel > target.maxLevel) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 前置 '${requirement.nodeId}' 的 minLevel 超过节点上限")
                }
                requirements.add(requirement)
            }
            return requirements
        }

        private fun parseRequirement(treeId: String, nodeId: String, rawRequirement: Any): SkillTreeNodeRequirement {
            if (rawRequirement !is Map<*, *>) {
                error("SkillTree '$treeId' 的节点 '$nodeId' 前置必须使用 node/minLevel 对象")
            }
            val rawNodeId = rawRequirement["node"]
            if (rawNodeId == null || rawNodeId.toString().isBlank()) {
                error("SkillTree '$treeId' 的节点 '$nodeId' 前置缺少 node")
            }
            val rawMinLevel = rawRequirement["minLevel"]
            val minLevel = if (rawMinLevel == null) {
                1
            } else {
                rawMinLevel.toString().toIntOrNull() ?: error("SkillTree '$treeId' 的节点 '$nodeId' 前置 minLevel 无效")
            }
            if (minLevel <= 0) {
                error("SkillTree '$treeId' 的节点 '$nodeId' 前置 minLevel 必须大于 0")
            }
            return SkillTreeNodeRequirement(rawNodeId.toString(), minLevel)
        }

        private fun validateNodeLevels(treeId: String, nodes: Map<String, SkillTreeNode>) {
            for ((nodeId, node) in nodes) {
                for (level in 1..node.maxLevel) {
                    if (!node.levels.containsKey(level)) {
                        error("SkillTree '$treeId' 的节点 '$nodeId' 缺少 Lv$level 条件")
                    }
                }
            }
        }

        private fun validateAcyclic(treeId: String, graph: Map<String, List<SkillTreeNodeRequirement>>) {
            val visiting = mutableSetOf<String>()
            val visited = mutableSetOf<String>()
            for (nodeId in graph.keys) {
                validateNodeAcyclic(treeId, nodeId, graph, visiting, visited)
            }
        }

        private fun validateNodeAcyclic(
            treeId: String,
            nodeId: String,
            graph: Map<String, List<SkillTreeNodeRequirement>>,
            visiting: MutableSet<String>,
            visited: MutableSet<String>
        ) {
            if (visited.contains(nodeId)) {
                return
            }
            if (!visiting.add(nodeId)) {
                error("SkillTree '$treeId' 的 graph 存在循环，节点 '$nodeId' 重复进入")
            }
            val requirements = graph[nodeId] ?: emptyList()
            for (requirement in requirements) {
                validateNodeAcyclic(treeId, requirement.nodeId, graph, visiting, visited)
            }
            visiting.remove(nodeId)
            visited.add(nodeId)
        }
    }
}
