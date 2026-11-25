package com.gitee.planners.module.action.rule

import java.util.Locale

/**
 * 过滤规则注册表。
 *
 * 内置规则由 [install] 登记；外部插件按 `registerExternal` 登记，停用时用
 * `unregisterAll` 整体撤销，与动作注册表的依赖方向、生命周期一致。
 * 规则主键按域分组（`health.*`、`team.*`、`distance`），因此不设别名。
 * `team.same` 由 FightCore 注册：严格同队需要 Creator 队伍 API，Planners 自身不依赖它。
 */
object FilterRules {

    /** 已登记的规则，按登记顺序保存以便诊断输出。 */
    private val rules = LinkedHashMap<String, FilterRule>()

    /** 外部所有者到撤销动作的映射。 */
    private val externalOwners = LinkedHashMap<Any, MutableList<() -> Unit>>()

    /** 内置规则是否已安装。 */
    private var installed = false

    /**
     * 安装内置规则。
     *
     * 可重复调用；安装完成后 `registerExternal` 才允许登记外部规则。
     */
    @Synchronized
    fun install() {
        if (installed) {
            return
        }
        // 先置位再登记，避免内置规则登记时递归回到本方法。
        installed = true
        register(HealthAliveRule)
        register(HealthDamagedRule)
        register(HealthFullRule)
        register(HealthRatioRule)
        register(TeamAllyRule)
        register(TeamEnemyRule)
        register(DistanceRule)
    }

    /**
     * 清空并重新安装内置规则，供测试隔离使用。
     */
    @Synchronized
    fun reset() {
        externalOwners.clear()
        rules.clear()
        installed = false
        install()
    }

    /**
     * 登记一条规则，同主键覆盖。
     *
     * @param rule 规则实现。
     */
    @Synchronized
    fun register(rule: FilterRule) {
        install()
        rules[keyOf(rule.id)] = rule
    }

    /**
     * 由外部插件登记规则，登记到所有者名下。
     *
     * @param owner 注册方，通常是插件实例。
     * @param rule 规则实现。
     */
    @Synchronized
    fun registerExternal(owner: Any, rule: FilterRule) {
        register(rule)
        track(owner) {
            remove(rule)
        }
    }

    /**
     * 撤销某个所有者登记的全部规则。
     *
     * @param owner 注册方。
     */
    @Synchronized
    fun unregisterAll(owner: Any) {
        val pending = externalOwners.remove(owner)
        if (pending == null) {
            return
        }
        for (removal in pending) {
            removal()
        }
    }

    /**
     * 按主键查找规则。
     *
     * @param id 规则主键。
     * @return 规则实现；未登记时返回 null。
     */
    fun rule(id: String): FilterRule? {
        return rules[keyOf(id)]
    }

    /**
     * 按主键查找规则，未登记时报错。
     *
     * @param id 规则主键。
     * @param path 配置路径，用于错误信息。
     * @return 规则实现。
     * @throws IllegalArgumentException 主键未登记时抛出。
     */
    fun require(id: String, path: String): FilterRule {
        val rule = rule(id)
        if (rule == null) {
            throw IllegalArgumentException("$path references unknown filter rule '$id', expected one of: $ids")
        }
        return rule
    }

    /**
     * 当前全部规则主键，用于诊断输出。
     */
    val ids: Set<String>
        get() {
            return rules.keys
        }

    /**
     * 返回当前已登记的外部所有者数量，用于诊断输出。
     *
     * @return 所有者数量。
     */
    fun externalOwnerCount(): Int {
        return externalOwners.size
    }

    /**
     * 登记一项撤销动作。
     *
     * @param owner 注册方。
     * @param removal 撤销动作。
     */
    @Synchronized
    private fun track(owner: Any, removal: () -> Unit) {
        val list = externalOwners[owner]
        if (list == null) {
            externalOwners[owner] = mutableListOf(removal)
            return
        }
        list.add(removal)
    }

    /** 按主键撤销一条规则。 */
    private fun remove(rule: FilterRule) {
        rules.remove(keyOf(rule.id))
    }

    /** 规则主键规范化。 */
    private fun keyOf(id: String): String {
        return id.trim().lowercase(Locale.ROOT)
    }
}
