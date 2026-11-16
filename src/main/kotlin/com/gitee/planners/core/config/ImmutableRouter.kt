package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import com.gitee.planners.api.common.Unique
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.util.mapSectionNotNull
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration

class ImmutableRouter(private val config: Configuration) : Unique {

    override val id = config.file!!.nameWithoutExtension

    val name = config.getString("__option__.name", id)!!

    /**
     * 写在 `algorithm.level` 下的内联等级算法，旧配置继续可用。
     */
    private val inlineLevelAlgorithm = Algorithm.parse(config.getConfigurationSection("__option__.algorithm.level"))

    /**
     * `algorithm.level` 引用的等级算法 ID。
     *
     * 语义与 `settings.level.algorithm` 一致，指向 `module/level` 下的算法键；
     * 只在访问时查注册表，避免依赖注册表加载顺序与热重载顺序。
     */
    val levelAlgorithmReference: String? =
        if (config.isConfigurationSection("__option__.algorithm.level")) {
            null
        } else {
            readLevelAlgorithmReference()
        }

    /**
     * 本路由使用的等级算法。
     *
     * 内联算法优先；否则按引用 ID 查 `module/level` 注册表，未配置时返回 null，
     * 由调用方回落到 `settings.level.algorithm`。
     */
    val algorithmLevel: Algorithm?
        get() {
            if (inlineLevelAlgorithm != null) {
                return inlineLevelAlgorithm
            }
            val reference = levelAlgorithmReference
            if (reference == null) {
                return null
            }
            return Registries.LEVEL.getOrNull(reference)
        }

    /**
     * 读取 `algorithm.level` 的算法 ID。
     *
     * @return 已去掉首尾空白的 ID；未配置或为空时返回 null。
     */
    private fun readLevelAlgorithmReference(): String? {
        val raw = config.getString("__option__.algorithm.level")
        if (raw == null) {
            return null
        }
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        return trimmed
    }

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

        for (route in routes.values) {
            for (childId in route.branchIds) {
                val child = getRouteOrNull(childId)
                if (child == null) {
                    error("Router '$id' 的 Job '${route.id}' 引用了不存在的子 Job '$childId'")
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

        // visited 仅统计可达节点；汇合节点仍须沿每条路径校验祖先技能和循环。
        visited.add(route.id)
        val nextAncestorSkills = ancestorSkills + job.skillIds
        for (child in route.getBranches()) {
            validateRoute(child, nextAncestorSkills, visited, stack)
        }
        stack.remove(route.id)
    }

}
