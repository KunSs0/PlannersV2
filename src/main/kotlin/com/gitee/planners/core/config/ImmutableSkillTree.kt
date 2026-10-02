package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import taboolib.library.configuration.ConfigurationSection

class ImmutableSkillTree(
    val id: String,
    val name: String,
    val clazz: String,
    val type: TreeType,
    val nodes: Map<String, SkillTreeNode>,
    val graph: Map<String, Graph>
) {

    class Graph(
        val groups: Map<String, Group>,
        val edges: List<Edge>
    ) {
        class Group(
            val id: String,
            val mode: Mode,
            val requirements: Map<String, Int>
        )

        class Edge(
            val nodeId: String,
            val minLevel: Int
        )

        enum class Mode {
            ALL,
            ANY
        }
    }

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
                SkillTreeNodeType.CHOICE -> parseChoiceNode(treeId, nodeId, config, maxLevel, levels, position)
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

        private fun parseChoiceNode(
            treeId: String,
            nodeId: String,
            config: ConfigurationSection,
            maxLevel: Int,
            levels: Map<Int, Map<String, Map<String, Any>>>,
            position: SkillTreeNodePosition
        ): ChoiceNode {
            val optionsSection = config.getConfigurationSection("options")
            if (optionsSection == null || optionsSection.getKeys(false).isEmpty()) {
                error("SkillTree '$treeId' choice node '$nodeId' requires options")
            }
            val options = LinkedHashMap<String, ChoiceNode.Option>()
            for (optionId in optionsSection.getKeys(false)) {
                val optionSection = optionsSection.getConfigurationSection(optionId)
                if (optionSection == null) {
                    error("SkillTree '$treeId' choice node '$nodeId' option '$optionId' must be section")
                }
                val name = optionSection.getString("name") ?: optionId
                val providerId = optionSection.getString("provider")
                val propertiesSection = optionSection.getConfigurationSection("properties")
                val properties = LinkedHashMap<String, Any>()
                if (propertiesSection != null) {
                    for ((key, value) in propertiesSection.getValues(false)) {
                        if (value != null) {
                            properties[key] = value
                        }
                    }
                }
                options[optionId] = ChoiceNode.Option(optionId, name, providerId, properties)
            }
            return ChoiceNode(nodeId, maxLevel, levels, position, options)
        }

        private fun parseGraph(
            treeId: String,
            config: ConfigurationSection,
            nodes: Map<String, SkillTreeNode>
        ): Map<String, Graph> {
            val section = config.getConfigurationSection("graph")
            if (section == null) {
                error("SkillTree '$treeId' 缺少 graph 节点")
            }
            val graph = LinkedHashMap<String, Graph>()
            for (nodeId in nodes.keys) {
                if (!section.contains(nodeId)) {
                    error("SkillTree '$treeId' 的 graph 缺少节点 '$nodeId'")
                }
            }
            for (nodeId in section.getKeys(false)) {
                if (!nodes.containsKey(nodeId)) {
                    error("SkillTree '$treeId' 的 graph 包含未知节点 '$nodeId'")
                }
                graph[nodeId] = parseNodeGraph(treeId, nodeId, section, nodes)
            }
            validateAcyclic(treeId, graph)
            return graph
        }

        private fun parseNodeGraph(
            treeId: String,
            nodeId: String,
            graphSection: ConfigurationSection,
            nodes: Map<String, SkillTreeNode>
        ): Graph {
            val nodeSection = graphSection.getConfigurationSection(nodeId)
            if (nodeSection == null) {
                error("SkillTree '$treeId' 的节点 '$nodeId' graph 必须使用配置节")
            }
            val groupsSection = nodeSection.getConfigurationSection("groups")
            if (groupsSection == null || groupsSection.getKeys(false).isEmpty()) {
                error("SkillTree '$treeId' 的节点 '$nodeId' 缺少 graph.groups")
            }
            val groups = LinkedHashMap<String, Graph.Group>()
            val edges = ArrayList<Graph.Edge>()
            for (groupId in groupsSection.getKeys(false)) {
                val groupSection = groupsSection.getConfigurationSection(groupId)
                if (groupSection == null) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 分支 '$groupId' 必须使用配置节")
                }
                val rawMode = groupSection.getString("mode")
                if (rawMode == null) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 分支 '$groupId' 缺少 mode")
                }
                val mode = try {
                    Graph.Mode.valueOf(rawMode.uppercase())
                } catch (exception: IllegalArgumentException) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 分支 '$groupId' mode 无效: $rawMode")
                }
                val requirementSection = groupSection.getConfigurationSection("requirements")
                if (requirementSection == null) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 分支 '$groupId' 缺少 requirements")
                }
                if (requirementSection.getKeys(false).isEmpty() && mode == Graph.Mode.ANY) {
                    error("SkillTree '$treeId' 的节点 '$nodeId' 分支 '$groupId' 的 ANY 分支不能为空")
                }
                val requirements = LinkedHashMap<String, Int>()
                for (requirementNodeId in requirementSection.getKeys(false)) {
                    val minLevel = requirementSection.getInt(requirementNodeId)
                    if (minLevel <= 0) {
                        error("SkillTree '$treeId' 的节点 '$nodeId' 前置 '$requirementNodeId' 等级必须大于 0")
                    }
                    val target = nodes[requirementNodeId]
                    if (target == null) {
                        error("SkillTree '$treeId' 的节点 '$nodeId' 引用了未知前置 '$requirementNodeId'")
                    }
                    if (minLevel > target.maxLevel) {
                        error("SkillTree '$treeId' 的节点 '$nodeId' 前置 '$requirementNodeId' 的 minLevel 超过节点上限")
                    }
                    if (requirements.containsKey(requirementNodeId)) {
                        error("SkillTree '$treeId' 的节点 '$nodeId' 分支 '$groupId' 重复前置 '$requirementNodeId'")
                    }
                    requirements[requirementNodeId] = minLevel
                    edges.add(Graph.Edge(requirementNodeId, minLevel))
                }
                groups[groupId] = Graph.Group(groupId, mode, requirements)
            }
            return Graph(groups, edges)
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

        private fun validateAcyclic(treeId: String, graph: Map<String, Graph>) {
            val visiting = mutableSetOf<String>()
            val visited = mutableSetOf<String>()
            for (nodeId in graph.keys) {
                validateNodeAcyclic(treeId, nodeId, graph, visiting, visited)
            }
        }

        private fun validateNodeAcyclic(
            treeId: String,
            nodeId: String,
            graph: Map<String, Graph>,
            visiting: MutableSet<String>,
            visited: MutableSet<String>
        ) {
            if (visited.contains(nodeId)) {
                return
            }
            if (!visiting.add(nodeId)) {
                error("SkillTree '$treeId' 的 graph 存在循环，节点 '$nodeId' 重复进入")
            }
            val nodeGraph = graph[nodeId]
            if (nodeGraph != null) {
                for (edge in nodeGraph.edges) {
                    validateNodeAcyclic(treeId, edge.nodeId, graph, visiting, visited)
                }
            }
            visiting.remove(nodeId)
            visited.add(nodeId)
        }
    }
}
