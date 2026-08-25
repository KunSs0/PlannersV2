package com.gitee.planners.core.config

import com.gitee.planners.api.common.Unique
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.util.mapSectionNotNull
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration

class ImmutableRouter(private val config: Configuration) : Unique {

    override val id = config.file!!.nameWithoutExtension

    val name = config.getString("__option__.name", id)!!

    val algorithmLevel =
        Algorithm.parse(config.getConfigurationSection("__option__.algorithm.level"))

    /**
     * 职业系 Bukkit 图标。
     *
     * 该对象仅由传统 Bukkit GUI 按需读取，加载配置时不创建 ItemStack。
     */
    val icon
        get() = config.getItemStack("__option__.icon")

    val routes = config.mapSectionNotNull {
        if (it.name == "__option__") return@mapSectionNotNull null
        ImmutableRoute(this, it)
    }

    val originate = if (config.isString("__option__.originate")) {
        getRouteOrNull(config.getString("__option__.originate")!!)
    } else {
        routes.values.firstOrNull()
    }

    init {
        validateRouteGraph()
    }

    fun getRouteOrNull(id: String): ImmutableRoute? {
        return routes[id]
    }

    fun getRouteByJob(job: ImmutableJob): ImmutableRoute? {
        return routes[job.id]
    }

    private fun validateRouteGraph() {
        val root = originate
        if (root == null) {
            error("Router '$id' 未定义起始 Job")
        }

        val parentByRoute = mutableMapOf<String, String>()
        for (route in routes.values) {
            for (childId in route.branchIds) {
                val child = getRouteOrNull(childId)
                if (child == null) {
                    error("Router '$id' 的 Job '${route.id}' 引用了不存在的子 Job '$childId'")
                }
                val previousParent = parentByRoute.putIfAbsent(childId, route.id)
                if (previousParent != null && previousParent != route.id) {
                    error("Router '$id' 的 Job '$childId' 存在多个父 Job: '$previousParent', '${route.id}'")
                }
            }
        }

        val visited = mutableSetOf<String>()
        validateRoute(root, emptySet(), visited, mutableSetOf())
        if (visited.size != routes.size) {
            val unreachable = routes.keys.filter { !visited.contains(it) }
            error("Router '$id' 存在不可达 Job: ${unreachable.joinToString(", ")}")
        }
    }

    private fun validateRoute(
        route: ImmutableRoute,
        ancestorSkills: Set<String>,
        visited: MutableSet<String>,
        stack: MutableSet<String>
    ) {
        if (!stack.add(route.id)) {
            error("Router '$id' 的 Job 转职图存在循环: ${stack.joinToString(" -> ")} -> ${route.id}")
        }

        val job = route.getJob()
        val duplicateSkills = job.skillIds.filter { ancestorSkills.contains(it) }.toSet()
        if (duplicateSkills.isNotEmpty()) {
            error(
                "Router '$id' 的子 Job '${route.id}' 与父 Job 技能冲突: " +
                    duplicateSkills.joinToString(", ")
            )
        }

        for (skillTreeId in route.skillTreeIds) {
            val skillTree = com.gitee.planners.api.Registries.SKILL_TREE.getOrNull(skillTreeId)
            if (skillTree == null) {
                error("Router '$id' 的 Job '${route.id}' 绑定了不存在的技能树 '$skillTreeId'")
            }
            for (node in skillTree.nodes.values) {
                if (node is SkillTreeSkillNode) {
                    val skill = job.getSkillOrNull(node.skillId)
                    if (skill == null) {
                        error(
                            "Router '$id' 的 Job '${route.id}' 技能树 '$skillTreeId' 包含非本阶段技能: " +
                                node.skillId
                        )
                    }
                    if (node.maxLevel > skill.maxLevel) {
                        error("Router '$id' 的技能节点 '${node.id}' 上限超过技能 '${node.skillId}' 的最高等级")
                    }
                    val isActive = skill.categories.contains("active")
                    val isPassive = skill.categories.contains("passive")
                    if (isActive && isPassive) {
                        error("Router '$id' 的技能 '${node.skillId}' 同时属于 active 和 passive 分类")
                    }
                }
            }
        }

        visited.add(route.id)
        val nextAncestorSkills = ancestorSkills + job.skillIds
        for (child in route.getBranches()) {
            validateRoute(child, nextAncestorSkills, visited, stack)
        }
        stack.remove(route.id)
    }

}
