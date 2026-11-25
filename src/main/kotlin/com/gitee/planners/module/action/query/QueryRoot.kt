package com.gitee.planners.module.action.query

/**
 * 查询路径的根节点。
 *
 * 每个根的后续分段语义不同，由 [QueryPath] 按根分派到对应的读取器，
 * 因此根只负责标识与作用域约束。
 *
 * @property id 配置中出现的关键字，小写。
 * @property skillOnly 是否只允许出现在技能流程中。
 * @property bareAllowed 是否允许只写根名（`<sender>` / `<caster>` 直接取对象本身）。
 */
enum class QueryRoot(
    val id: String,
    val skillOnly: Boolean = false,
    val bareAllowed: Boolean = false,
) {

    /**
     * 当前执行上下文的施法者，字段按运行期实际类型分派。
     *
     * 逐目标 `call` 命中玩家目标时会改写成该玩家；要读"最初发起本次施法的人"，
     * 必须用 [CASTER]。
     */
    SENDER("sender", bareAllowed = true),

    /**
     * 本次施法的施法者，整个执行周期不变。
     *
     * 与 [SENDER] 的差别只在逐目标子流程：那里 `sender` 是当前元素（可能已被换成目标玩家），
     * `caster` 仍是发起者。拼「施法者标识」这类 key 时必须用这个根，否则同一个目标会被
     * 多个施法者共用一把 key。
     */
    CASTER("caster", bareAllowed = true),

    /** 当前目标，即最近一个目标器解析出的接收者。 */
    TARGET("target", true),

    /** 技能自身，可读取元数据与本次已计算的变量。 */
    SKILL("skill"),

    /** 玩家档案，读取等级、经验与职业。 */
    PROFILE("profile"),

    /** 玩家身上的共享标记，剩余整体作为 key。 */
    DATA("data"),

    /** 战斗属性，含装备与 Buff 来源。 */
    ATTR("attr"),

    /** 技能树节点学习等级，要求 `<树ID>.<节点ID>`。 */
    NODE("node", true),

    /**
     * 施法者持有的货币余额。
     *
     * 需要 `<货币ID>`，ID 取自 `module/currency` 下的货币配置；
     * 读取走货币实现自身的余额接口，未注册的货币返回缺失。
     */
    ECONOMY("economy"),

    /**
     * 区域实例。
     *
     * 两种写法：`<area.{分类}.count|owner>` 查询覆盖当前作用点的实例，
     * `<area.self.*>` 只在区域回调流程内有效，读取当前实例自身。
     */
    AREA("area", true),

    /**
     * 当前行选择器解析出的目标。
     *
     * 只提供 inder.now：返回本行目标的实体 UUID，逗号分隔，空行返回空串。
     * 与 @uuid 目标器配合，可把一次选择存进标记后再在另一次执行里还原。
     */
    FINDER("finder", true);

    companion object {

        /**
         * 按关键字查找根节点。
         *
         * @param value 配置中出现的关键字。
         * @return 匹配的根；未知时返回 null。
         */
        fun of(value: String): QueryRoot? {
            val key = value.trim().lowercase()
            for (root in values()) {
                if (root.id == key) {
                    return root
                }
            }
            return null
        }

        /**
         * 全部根关键字，用于错误信息。
         *
         * @return 以逗号分隔的关键字列表。
         */
        fun keywords(): String {
            val builder = StringBuilder()
            for (root in values()) {
                if (builder.isNotEmpty()) {
                    builder.append(", ")
                }
                builder.append(root.id)
            }
            return builder.toString()
        }
    }
}
