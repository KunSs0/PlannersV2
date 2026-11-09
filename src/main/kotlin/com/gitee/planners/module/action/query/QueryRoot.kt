package com.gitee.planners.module.action.query

/**
 * 查询路径的根节点。
 *
 * 每个根的后续分段语义不同，由 [QueryPath] 按根分派到对应的读取器，
 * 因此根只负责标识与作用域约束。
 *
 * @property id 配置中出现的关键字，小写。
 * @property skillOnly 是否只允许出现在技能流程中。
 */
enum class QueryRoot(val id: String, val skillOnly: Boolean = false) {

    /** 施法者，字段按运行期实际类型分派。 */
    SENDER("sender"),

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

    /** 职业自然成长，只含职业成长，不含装备与 Buff。 */
    NATURAL("natural"),

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
     * 本次连招播放的变量。
     *
     * 由回调载荷里的播放上下文提供，随播放销毁，不跨施法保留。
     */
    PLAYBACK("playback", true),

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
