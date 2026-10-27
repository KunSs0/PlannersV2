package com.gitee.planners.core.value

/**
 * 数值修正的触发条件。
 *
 * 当前只支持累计等级区间：`type: level` 搭配 `props.min` 与 `props.max`。
 *
 * @property minimum 生效的最小等级。
 * @property maximum 生效的最大等级。
 */
data class LevelCondition(val minimum: Int, val maximum: Int) {

    /**
     * 判断等级是否命中区间。
     *
     * @param level 当前等级。
     * @return 命中返回 true。
     */
    fun matches(level: Int): Boolean {
        return level >= minimum && level <= maximum
    }

    companion object {

        private const val TYPE = "level"

        /**
         * 解析等级条件，拒绝其他条件类型与脚本条件。
         *
         * @param raw 原始配置节点。
         * @param path 配置路径。
         * @return 等级条件。
         * @throws IllegalArgumentException 结构、字段或区间非法时携带配置路径抛出。
         */
        fun parse(raw: Any?, path: String): LevelCondition {
            require(raw is Map<*, *>) { "$path 必须是对象" }
            for (key in raw.keys) {
                require(key is String && (key == "type" || key == "props" || key == "script")) {
                    "$path 包含未知字段 '$key'"
                }
            }
            require(raw["type"] == TYPE) { "$path.type 仅支持 level，实际为 '${raw["type"]}'" }
            require(!raw.containsKey("script")) { "$path.script 不支持脚本条件" }
            val props = raw["props"]
            require(props is Map<*, *>) { "$path.props 必须是包含 min 与 max 的对象" }
            for (key in props.keys) {
                require(key is String && (key == "min" || key == "max")) {
                    "$path.props 包含未知字段 '$key'"
                }
            }
            val minimum = requireLevel(props["min"], "$path.props.min")
            val maximum = requireLevel(props["max"], "$path.props.max")
            require(maximum >= minimum) { "$path.props.max 不能小于 min" }
            return LevelCondition(minimum, maximum)
        }

        /**
         * 读取正整数等级。
         *
         * @param raw 原始配置值。
         * @param path 配置路径。
         * @return 等级整数。
         */
        private fun requireLevel(raw: Any?, path: String): Int {
            require(raw is Number) { "$path 必须是整数" }
            val value = raw.toDouble()
            require(value.isFinite() && value == kotlin.math.floor(value)) { "$path 必须是整数" }
            require(value >= 1.0 && value <= Int.MAX_VALUE.toDouble()) { "$path 必须是正整数" }
            return value.toInt()
        }
    }
}
