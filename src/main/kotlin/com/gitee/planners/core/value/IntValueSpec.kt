package com.gitee.planners.core.value

/**
 * 整数规格：基础值加一组按声明顺序生效的修正。
 *
 * 结构为 `base/modifiers/update`；`update` 只声明刷新依赖，由使用方决定是否支持。
 *
 * @property base 基础值。
 * @property modifiers 有序修正列表。
 * @property update 声明的刷新依赖。
 */
data class IntValueSpec(
    val base: Int,
    val modifiers: List<IntValueModifier>,
    val update: List<String> = emptyList(),
) {

    /**
     * 在指定上下文中求值，整数在全部修正完成后检查范围并向零截断。
     *
     * @param context 条件判断使用的上下文。
     * @param matches 条件判断回调。
     * @return 求值结果。
     * @throws IllegalArgumentException 条件不匹配、运算非法或结果越界时抛出。
     */
    fun resolve(context: Int, matches: (LevelCondition, Int) -> Boolean): Int {
        val operations = ArrayList<String>()
        for (modifier in modifiers) {
            val condition = modifier.condition
            if (condition == null || matches(condition, context)) {
                operations.add(modifier.operation)
            }
        }
        val value = IntValueOperation.resolve(base.toDouble(), operations)
        require(value >= Int.MIN_VALUE.toDouble() && value <= Int.MAX_VALUE.toDouble()) {
            "解析后的整数数值超出范围：$value"
        }
        return value.toInt()
    }

    companion object {

        private val FIELDS = setOf("base", "modifiers", "update")

        /**
         * 解析字面量或 `base/modifiers/update` 结构。
         *
         * @param raw 原始配置节点。
         * @param path 配置路径。
         * @return 整数规格。
         * @throws IllegalArgumentException 字段未知、类型错误或数值非法时携带配置路径抛出。
         */
        fun parse(raw: Any?, path: String): IntValueSpec {
            if (raw !is Map<*, *>) {
                return IntValueSpec(requireBase(raw, path), emptyList())
            }
            for (key in raw.keys) {
                require(key is String && key in FIELDS) { "$path 包含未知字段 '$key'" }
            }
            require(raw.containsKey("base")) { "$path.base 不能为空" }
            val base = requireBase(raw["base"], "$path.base")
            val modifiers = parseModifiers(raw["modifiers"], "$path.modifiers")
            val update = parseUpdate(raw["update"], "$path.update")
            return IntValueSpec(base, modifiers, update)
        }

        /**
         * 读取 Int 范围内的整数字面量。
         *
         * @param raw 原始配置值。
         * @param path 配置路径。
         * @return 基础值。
         */
        private fun requireBase(raw: Any?, path: String): Int {
            require(raw is Number || raw is String) { "$path 必须是有限数字" }
            val value = raw.toString().trim().toDoubleOrNull()
            require(value != null && value.isFinite()) { "$path 必须是有限数字" }
            require(value == kotlin.math.floor(value)) { "$path 必须是 Int 范围内的整数" }
            require(value >= Int.MIN_VALUE.toDouble() && value <= Int.MAX_VALUE.toDouble()) {
                "$path 必须是 Int 范围内的整数"
            }
            return value.toInt()
        }

        /**
         * 解析命名修正，保留声明顺序。
         *
         * @param raw 原始配置节点。
         * @param path 配置路径。
         * @return 修正列表。
         */
        private fun parseModifiers(raw: Any?, path: String): List<IntValueModifier> {
            if (raw == null) {
                return emptyList()
            }
            require(raw is Map<*, *>) { "$path 必须是命名对象" }
            val result = ArrayList<IntValueModifier>()
            for ((id, entry) in raw) {
                require(id is String && id.isNotBlank()) { "$path 的修正 ID 不能为空" }
                require(entry is Map<*, *>) { "$path.$id 必须是对象" }
                for (key in entry.keys) {
                    require(key is String && (key == "condition" || key == "operation")) {
                        "$path.$id 包含未知字段 '$key'"
                    }
                }
                val operation = entry["operation"]
                require(operation is String && operation.isNotBlank()) { "$path.$id.operation 不能为空" }
                try {
                    IntValueOperation.apply(0.0, operation)
                } catch (error: IllegalArgumentException) {
                    throw IllegalArgumentException("$path.$id.operation 无效：${error.message}", error)
                }
                var condition: LevelCondition? = null
                if (entry.containsKey("condition")) {
                    condition = LevelCondition.parse(entry["condition"], "$path.$id.condition")
                }
                result.add(IntValueModifier(id, condition, operation))
            }
            return result
        }

        /**
         * 解析刷新依赖声明。
         *
         * @param raw 原始配置节点。
         * @param path 配置路径。
         * @return 更新键列表。
         */
        private fun parseUpdate(raw: Any?, path: String): List<String> {
            if (raw == null) {
                return emptyList()
            }
            require(raw is List<*>) { "$path 必须是字符串列表" }
            val result = ArrayList<String>()
            for (key in raw) {
                require(key is String && key.isNotBlank()) { "$path 的更新键必须是非空字符串" }
                result.add(key.trim())
            }
            return result
        }
    }
}
