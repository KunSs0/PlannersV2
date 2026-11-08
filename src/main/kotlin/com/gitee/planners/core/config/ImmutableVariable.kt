package com.gitee.planners.core.config

import com.gitee.planners.api.job.Variable
import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration

/**
 * 技能变量定义。
 *
 * 常量直接返回原值；字符串、表达式与条件分支统一交给查询引擎求值。
 */
interface ImmutableVariable : Variable {

    /**
     * 用查询表达式求值本变量。
     *
     * 供结构化动作使用：常量直接返回原值，表达式与条件分支交给查询引擎。
     *
     * @param scope 查询上下文，已包含此前声明的变量。
     * @return 变量值。
     */
    fun evaluate(scope: QueryScope): Any?

    companion object {
        private val identifierPattern = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

        /**
         * 解析配置，字符串与基本类型直接作为常量，映射必须显式声明表达式。
         *
         * @param id 上下文变量名。
         * @param value YAML 中的原始值。
         * @return 常量、显式表达式或已有的条件分支定义。
         * @throws IllegalArgumentException 名称或表达式结构不合法时抛出。
         */
        fun parse(id: String, value: Any): ImmutableVariable {
            require(identifierPattern.matches(id)) {
                "变量 '$id' 必须是合法的变量标识符。"
            }
            if (value is ConfigurationSection) {
                return parseExpression(id, value.getValues(false))
            }
            if (value is Map<*, *>) {
                return parseExpression(id, value)
            }
            if (value is List<*>) {
                val first = value.firstOrNull()
                if (first is Map<*, *>) {
                    val branches = ArrayList<ConfigurationSection>()
                    for (branch in value) {
                        require(branch is Map<*, *>) { "变量 '$id' 的条件分支必须全部为映射。" }
                        branches.add(Configuration.fromMap(branch))
                    }
                    return When(id, branches)
                }
                return Literal(id, copyLiteralList(value))
            }
            require(value is String || value is Boolean || value is Number) {
                "变量 '$id' 不支持配置类型 ${value::class.java.name}。"
            }
            if (value is String) {
                // 字符串统一交给查询引擎判定：路径 / 表达式 / 模板 / 文本，无需 expression 包装。
                return Auto(id, value)
            }
            return Literal(id, value)
        }

        /**
         * 为每次求值复制列表，避免 action 修改数据后污染下一次施法的配置。
         *
         * @param values 原始常量列表。
         * @return 独立的递归列表快照。
         * @throws IllegalArgumentException 列表包含不支持的对象类型时抛出。
         */
        private fun copyLiteralList(values: List<*>): List<Any?> {
            val copy = ArrayList<Any?>()
            for (value in values) {
                if (value is List<*>) {
                    copy.add(copyLiteralList(value))
                } else {
                    require(value == null || value is String || value is Boolean || value is Number) {
                        "变量常量列表只能包含字符串、数字、布尔、null 或嵌套列表。"
                    }
                    copy.add(value)
                }
            }
            return copy
        }

        /**
         * 解析唯一的显式表达式字段，拒绝拼写错误与额外字段。
         *
         * @param id 变量名。
         * @param fields 表达式结构。
         * @return 预编译表达式定义。
         * @throws IllegalArgumentException 字段不完整、为空或类型不正确时抛出。
         */
        private fun parseExpression(id: String, fields: Map<*, *>): ImmutableVariable {
            require(fields.size == 1 && fields.containsKey("expression")) {
                "变量 '$id' 的映射只允许 expression 字段。"
            }
            val expression = fields["expression"]
            require(expression is String && expression.isNotBlank()) {
                "变量 '$id.expression' 必须是非空表达式字符串。"
            }
            return Expression(id, expression)
        }
    }

    /**
     * 不参与代码编译的原始常量。
     *
     * @property id 变量名。
     * @property value YAML 原始常量值。
     */
    class Literal(override val id: String, val value: Any) : ImmutableVariable {

        /**
         * 常量直接返回配置值，列表返回副本避免调用方修改配置对象。
         *
         * @param scope 查询上下文。
         * @return 常量值。
         */
        override fun evaluate(scope: QueryScope): Any? {
            if (value is List<*>) {
                return ArrayList(value)
            }
            return value
        }
    }

    /**
     * 由文本自动判定的变量。
     *
     * 配置直接写标量即可：数字、布尔按常量，`skill.level` 按路径，
     * `10 * skill.level + 50` 按公式，`&a冲锋 Lv<skill.level>` 按模板，
     * 其余解析不成的按普通文本。
     *
     * 判定推迟到首次结构化求值，加载期只做语法校验。
     *
     * @property id 变量名。
     * @param source 配置中的原始文本。
     */
    class Auto(override val id: String, private val source: String) : ImmutableVariable {

        @Volatile
        private var compiledRef: QueryRef? = null


        /**
         * 按查询引擎求值。
         *
         * @param scope 查询上下文。
         * @return 变量值。
         */
        override fun evaluate(scope: QueryScope): Any? {
            return resolveRef().resolve(scope)
        }

        /**
         * 返回配置中的原始文本。
         *
         * @return 原始文本。
         */
        fun rawText(): String {
            return source
        }

        /** 取已编译引用，未编译时编译并缓存。 */
        private fun resolveRef(): QueryRef {
            val cached = compiledRef
            if (cached != null) {
                return cached
            }
            val created = QueryRef.parse(source, "variable:$id")
            compiledRef = created
            return created
        }
    }

    /**
     * 通过 expression 字段显式声明的计算表达式。
     *
     * 保留旧写法兼容；新配置直接写标量即可由 [Auto] 判定。
     *
     * @property id 变量名。
     * @property expression 待预编译的表达式。
     */
    class Expression(override val id: String, private val expression: String) : ImmutableVariable {

        /**
         * 首次结构化求值时编译并缓存。
         *
         * 表达式统一按查询表达式解析，但允许引用同技能已声明的变量，
         * 因此编译推迟到真正用到结构化求值的时候。
         */
        @Volatile
        private var compiledRef: QueryRef? = null


        /**
         * 用查询引擎求值表达式。
         *
         * 允许裸标识符：变量按声明顺序求值，后声明的可以直接引用先声明的名字
         * （例如 `castTime / 20.0`）。未声明的名字求值为空，不中断整个技能。
         *
         * @param scope 查询上下文。
         * @return 表达式结果。
         */
        override fun evaluate(scope: QueryScope): Any? {
            return resolveRef().resolve(scope)
        }

        /** 取已编译表达式，未编译时编译并缓存。 */
        private fun resolveRef(): QueryRef {
            val cached = compiledRef
            if (cached != null) {
                return cached
            }
            val created = QueryRef.parseVariableExpression(expression, "variable:$id")
            compiledRef = created
            return created
        }
    }

    /**
     * 按声明顺序匹配的条件变量；condition 与 action 本身就是显式表达式字段。
     *
     * @property id 变量名。
     * @param values 条件与结果表达式列表。
     */
    class When(override val id: String, values: List<ConfigurationSection>) : ImmutableVariable {
        private val expression = createExpression(values)

        /** 原始分支配置，首次结构化求值时才编译。 */
        private val rawValues = values

        /**
         * 首次结构化求值时编译并缓存；旧脚本写法不会触发编译。
         */
        @Volatile
        private var compiledBranches: List<Branch>? = null


        /**
         * 按声明顺序求值，全部不匹配时返回 false。
         *
         * @param scope 查询上下文。
         * @return 匹配分支的结果。
         */
        override fun evaluate(scope: QueryScope): Any? {
            for (branch in resolveBranches()) {
                if (branch.condition.resolveBoolean(scope)) {
                    return branch.action.resolve(scope)
                }
            }
            return false
        }

        /** 取已编译分支，未编译时编译并缓存。 */
        private fun resolveBranches(): List<Branch> {
            val cached = compiledBranches
            if (cached != null) {
                return cached
            }
            val created = createBranches(rawValues)
            compiledBranches = created
            return created
        }

        /**
         * 从末分支生成条件，保留未匹配返回 false 的既有条件变量契约。
         *
         * @param values 按配置顺序排列的分支。
         * @return 条件表达式。
         * @throws IllegalArgumentException 分支为空或缺少表达式时抛出。
         */
        private fun createExpression(values: List<ConfigurationSection>): String {
            require(values.isNotEmpty()) { "变量 '$id' 必须至少声明一个条件分支。" }
            var generated = "false"
            for (index in values.size - 1 downTo 0) {
                val section = values[index]
                val condition = section.getString("condition")
                require(condition != null && condition.isNotBlank()) { "变量 '$id' 的分支 $index 缺少 condition。" }
                val action = section.getString("action")
                require(action != null && action.isNotBlank()) { "变量 '$id' 的分支 $index 缺少 action。" }
                generated = "if ($condition) ($action) else ($generated)"
            }
            return generated
        }

        /**
         * 把分支编译成求值表达式对。
         *
         * @param values 按配置顺序排列的分支。
         * @return 已编译分支列表。
         */
        private fun createBranches(values: List<ConfigurationSection>): List<Branch> {
            val result = ArrayList<Branch>()
            var index = 0
            for (section in values) {
                val condition = section.getString("condition")
                val action = section.getString("action")
                if (condition == null || action == null) {
                    throw IllegalArgumentException("变量 '$id' 的分支 $index 缺少 condition 或 action。")
                }
                result.add(
                    Branch(
                        QueryRef.parseExpression(condition, "variable:$id.branch[$index].condition"),
                        QueryRef.parseExpression(action, "variable:$id.branch[$index].action"),
                    ),
                )
                index += 1
            }
            return result
        }
    }

    /**
     * 条件变量的一条已编译分支。
     *
     * @property condition 条件表达式。
     * @property action 条件成立时的结果表达式。
     */
    class Branch(val condition: QueryRef, val action: QueryRef)
}
