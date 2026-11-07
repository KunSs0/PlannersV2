package com.gitee.planners.module.action

import java.util.Locale

/**
 * 动作、条件、目标器与触发器的注册表。
 *
 * 不做运行期动态扩展：全部条目在插件启用阶段一次性注册，
 * 查找失败即视为配置错误。
 */
object ActionRegistry {

    private val actions = LinkedHashMap<String, Action>()

    private val conditions = LinkedHashMap<String, ActionCondition>()

    private val targeters = LinkedHashMap<String, ActionTargeter>()

    private val triggers = LinkedHashMap<String, ActionTrigger>()

    /**
     * 外部插件注册的条目，按所有者分组。
     *
     * 每个所有者持有撤销自己条目的动作，插件停用时整体撤销，避免重载后残留旧实例。
     */
    private val externalOwners = LinkedHashMap<Any, MutableList<() -> Unit>>()

    private var installed = false

    /**
     * 安装全部内置条目；重复调用不会重复注册。
     */
    @Synchronized
    fun install() {
        if (installed) {
            return
        }
        installed = true
        ActionBuiltins.install()
    }

    /**
     * 清空并重新安装内置条目，供测试隔离使用。
     */
    @Synchronized
    fun resetToBuiltins() {
        actions.clear()
        conditions.clear()
        targeters.clear()
        triggers.clear()
        installed = false
        install()
    }

    /**
     * 按主键或别名查找动作。
     *
     * @param id 动作主键或别名。
     * @return 动作实现；未注册时返回 null。
     */
    fun action(id: String): Action? {
        install()
        return actions[id.trim().lowercase(Locale.ROOT)]
    }

    /**
     * 按主键或别名查找条件。
     *
     * @param id 条件主键或别名。
     * @return 条件实现；未注册时返回 null。
     */
    fun condition(id: String): ActionCondition? {
        install()
        return conditions[id.trim().lowercase(Locale.ROOT)]
    }

    /**
     * 按主键或别名查找目标器。
     *
     * @param id 目标器主键或别名。
     * @return 目标器实现；未注册时返回 null。
     */
    fun targeter(id: String): ActionTargeter? {
        install()
        return targeters[id.trim().lowercase(Locale.ROOT)]
    }

    /**
     * 按主键或别名查找触发器。
     *
     * @param id 触发器主键或别名。
     * @return 触发器实现；未注册时返回 null。
     */
    fun trigger(id: String): ActionTrigger? {
        install()
        return triggers[id.trim().lowercase(Locale.ROOT)]
    }

    /**
     * 注册动作。
     *
     * @param action 动作实现。
     */
    fun register(action: Action) {
        install()
        for (alias in action.aliases) {
            val key = alias.trim().lowercase(Locale.ROOT)
            if (key.isNotEmpty()) {
                actions[key] = action
            }
        }
        actions[action.id.trim().lowercase(Locale.ROOT)] = action
    }

    /**
     * 注册条件。
     *
     * @param condition 条件实现。
     */
    fun register(condition: ActionCondition) {
        install()
        for (alias in condition.aliases) {
            val key = alias.trim().lowercase(Locale.ROOT)
            if (key.isNotEmpty()) {
                conditions[key] = condition
            }
        }
        conditions[condition.id.trim().lowercase(Locale.ROOT)] = condition
    }

    /**
     * 注册目标器。
     *
     * @param targeter 目标器实现。
     */
    fun register(targeter: ActionTargeter) {
        install()
        for (alias in targeter.aliases) {
            val key = alias.trim().lowercase(Locale.ROOT)
            if (key.isNotEmpty()) {
                targeters[key] = targeter
            }
        }
        targeters[targeter.id.trim().lowercase(Locale.ROOT)] = targeter
    }

    /**
     * 注册触发器。
     *
     * @param trigger 触发器实现。
     */
    fun register(trigger: ActionTrigger) {
        install()
        for (alias in trigger.aliases) {
            val key = alias.trim().lowercase(Locale.ROOT)
            if (key.isNotEmpty()) {
                triggers[key] = trigger
            }
        }
        triggers[trigger.id.trim().lowercase(Locale.ROOT)] = trigger
    }

    /**
     * 由外部插件注册动作，登记到所有者名下。
     *
     * 依赖方向保持单向：Planners 不引用外部插件，外部插件在自己的启用阶段调用本方法。
     * 插件停用时应调用 [unregisterAll] 撤销自己的全部条目。
     *
     * @param owner 注册方，通常是插件实例。
     * @param action 动作实现。
     */
    fun registerExternal(owner: Any, action: Action) {
        register(action)
        track(owner) {
            removeAction(action)
        }
    }

    /**
     * 由外部插件注册条件。
     *
     * @param owner 注册方。
     * @param condition 条件实现。
     */
    fun registerExternal(owner: Any, condition: ActionCondition) {
        register(condition)
        track(owner) {
            removeCondition(condition)
        }
    }

    /**
     * 由外部插件注册目标器。
     *
     * @param owner 注册方。
     * @param targeter 目标器实现。
     */
    fun registerExternal(owner: Any, targeter: ActionTargeter) {
        register(targeter)
        track(owner) {
            removeTargeter(targeter)
        }
    }

    /**
     * 由外部插件注册触发器。
     *
     * @param owner 注册方。
     * @param trigger 触发器实现。
     */
    fun registerExternal(owner: Any, trigger: ActionTrigger) {
        register(trigger)
        track(owner) {
            removeTrigger(trigger)
        }
    }

    /**
     * 撤销某个所有者注册的全部条目。
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

    /** 撤销一个动作的全部键。 */
    private fun removeAction(action: Action) {
        for (alias in action.aliases) {
            actions.remove(alias.trim().lowercase(Locale.ROOT))
        }
        actions.remove(action.id.trim().lowercase(Locale.ROOT))
    }

    /** 撤销一个条件的全部键。 */
    private fun removeCondition(condition: ActionCondition) {
        for (alias in condition.aliases) {
            conditions.remove(alias.trim().lowercase(Locale.ROOT))
        }
        conditions.remove(condition.id.trim().lowercase(Locale.ROOT))
    }

    /** 撤销一个目标器的全部键。 */
    private fun removeTargeter(targeter: ActionTargeter) {
        for (alias in targeter.aliases) {
            targeters.remove(alias.trim().lowercase(Locale.ROOT))
        }
        targeters.remove(targeter.id.trim().lowercase(Locale.ROOT))
    }

    /** 撤销一个触发器的全部键。 */
    private fun removeTrigger(trigger: ActionTrigger) {
        for (alias in trigger.aliases) {
            triggers.remove(alias.trim().lowercase(Locale.ROOT))
        }
        triggers.remove(trigger.id.trim().lowercase(Locale.ROOT))
    }

    /**
     * 返回全部动作主键，按注册顺序。
     *
     * @return 主键集合。
     */
    fun actionIds(): Set<String> {
        install()
        val result = LinkedHashSet<String>()
        for (action in actions.values) {
            result.add(action.id)
        }
        return result
    }

    /**
     * 返回全部条件主键，按注册顺序。
     *
     * @return 主键集合。
     */
    fun conditionIds(): Set<String> {
        install()
        val result = LinkedHashSet<String>()
        for (condition in conditions.values) {
            result.add(condition.id)
        }
        return result
    }

    /**
     * 返回全部目标器主键，按注册顺序。
     *
     * @return 主键集合。
     */
    fun targeterIds(): Set<String> {
        install()
        val result = LinkedHashSet<String>()
        for (targeter in targeters.values) {
            result.add(targeter.id)
        }
        return result
    }

    /**
     * 返回全部触发器主键，按注册顺序。
     *
     * @return 主键集合。
     */
    fun triggerIds(): Set<String> {
        install()
        val result = LinkedHashSet<String>()
        for (trigger in triggers.values) {
            result.add(trigger.id)
        }
        return result
    }
}
