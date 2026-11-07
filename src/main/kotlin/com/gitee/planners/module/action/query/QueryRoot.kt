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
     * 本次连招播放的变量。
     *
     * 由回调载荷里的播放上下文提供，随播放销毁，不跨施法保留。
     */
    PLAYBACK("playback", true);

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
