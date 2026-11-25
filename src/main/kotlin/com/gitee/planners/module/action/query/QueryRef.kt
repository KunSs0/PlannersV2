package com.gitee.planners.module.action.query

import com.gitee.action.api.query.QueryExpression

/**
 * 一个已解析的取值引用。
 *
 * 承担「字面量 / 纯路径 / 求值表达式」三种形态的统一入口：
 * - 字面量：直接返回常量，无任何解析开销
 * - 纯路径：按路径查表，不构造表达式树
 * - 求值表达式：交由 Action 的 [QueryExpression] 求值，路径解析回调到 [QueryPath]
 *
 * 形态在加载期判定一次并固化，运行期不再重新判定；结果不缓存，
 * 每次调用都按当前执行上下文现算。
 *
 * @property label 配置路径，用于错误信息。
 */
class QueryRef private constructor(
    private val literal: Any?,
    private val path: QueryPath?,
    private val expression: String?,
    private val template: QueryTemplate?,
    val label: String,
    private val source: String? = null,
    private val allowVariables: Boolean = false,
) {

    /**
     * 返回配置中的原始文本，用于需要整段公式的字段。
     *
     * @return 原始文本；非文本来源时返回字面量文本。
     */
    fun sourceText(): String? {
        if (source != null) {
            return source
        }
        if (literal == null) {
            return null
        }
        return literal.toString()
    }

    /**
     * 是否为字面量。
     *
     * @return 字面量时返回 true。
     */
    fun isLiteral(): Boolean {
        return path == null && expression == null && template == null
    }

    /**
     * 返回字面量值；非字面量返回 null。
     *
     * 供加载期读取静态参数使用（例如定时器周期的字面量写法）。
     *
     * @return 字面量值。
     */
    fun literalValue(): Any? {
        if (!isLiteral()) {
            return null
        }
        return literal
    }

    /**
     * 求值。
     *
     * @param scope 查询上下文。
     * @return 求值结果；缺失时返回 null。
     */
    fun resolve(scope: QueryScope): Any? {
        if (path != null) {
            return path.resolve(scope)
        }
        if (expression != null) {
            // 表达式由 Action 求值；路径解析回调到技能侧的字段语义
            return QueryExpression.parse(expression, label).evaluate { pathText ->
                resolvePath(pathText, scope)
            }
        }
        if (template != null) {
            return template.render(scope)
        }
        return literal
    }

    /**
     * 求值并转换为文本。
     *
     * @param scope 查询上下文。
     * @return 文本结果；缺失时返回空串。
     */
    fun resolveText(scope: QueryScope): String {
        return QueryText.renderValue(resolve(scope))
    }

    /**
     * 求值并转换为数值。
     *
     * @param scope 查询上下文。
     * @return 数值结果。
     * @throws IllegalArgumentException 结果不是数值时抛出。
     */
    fun resolveDouble(scope: QueryScope): Double {
        return QueryText.requireDouble(resolve(scope), label)
    }

    /**
     * 求值并转换为布尔。
     *
     * @param scope 查询上下文。
     * @throws IllegalArgumentException 结果不是布尔时抛出。
     * @return 布尔结果。
     */
    fun resolveBoolean(scope: QueryScope): Boolean {
        return QueryText.requireBoolean(resolve(scope), label)
    }

    /**
     * 解析表达式中的一条路径。
     *
     * 允许裸标识符的公式（等级表）把裸名按上下文变量表解析；其余场景按查询路径解析，
     * 裸名不是合法根时会抛出，从而保留"未知标识符在加载期报错"的旧语义。
     *
     * @param pathText 表达式里的路径文本。
     * @param scope 查询上下文。
     * @return 取值结果。
     */
    private fun resolvePath(pathText: String, scope: QueryScope): Any? {
        if (allowVariables && !pathText.contains('.')) {
            return resolveVariable(pathText, scope)
        }
        return QueryPath.parse(pathText, label).resolve(scope)
    }

    /**
     * 按上下文变量表解析裸标识符。
     *
     * 先按原名、再按小写名查变量表；`level`/`id`/`name` 允许回落到技能字段读取器，
     * 与迁移前的语义保持一致。
     *
     * @param name 裸标识符。
     * @param scope 查询上下文。
     * @return 变量值；缺失返回 null。
     */
    private fun resolveVariable(name: String, scope: QueryScope): Any? {
        if (scope.variables.containsKey(name)) {
            return scope.variables[name]
        }
        val key = name.lowercase(java.util.Locale.ROOT)
        if (scope.variables.containsKey(key)) {
            return scope.variables[key]
        }
        val skill = scope.skill
        if (skill == null) {
            return null
        }
        if (key == "level" || key == "id" || key == "name") {
            return scope.skillFieldReader(skill, key)
        }
        return null
    }

    companion object {

        /**
         * 解析一个参数取值。
         *
         * 判定顺序与 Creator 的 `QueryRef.parse` 一致：
         * - 带 `<>` 外壳：剥壳后按路径或表达式处理
         * - 含 `<路径>` 片段：按模板处理
         * - 纯路径且根已知：按路径处理
         * - 能解析成表达式：按表达式处理
         * - 其余：按字面量处理
         *
         * 最后一步与 Creator 略有不同：Planners 的变量没有声明类型，展示文本也写在这里，
         * 因此改用「解析成功才算公式」，解析失败回落字面量，避免 `&a冲锋` 这类文案在加载期报错。
         *
         * @param text 配置中的原始文本。
         * @param label 配置路径，用于错误信息。
         * @return 已解析的取值引用。
         * @throws IllegalArgumentException 语法非法时抛出。
         */
        fun parse(text: String, label: String): QueryRef {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("$label cannot be blank.")
            }
            if (QueryText.isWrapped(trimmed)) {
                val inner = QueryText.unwrap(trimmed, label)
                return parseQuery(inner, label)
            }
            if (QueryTemplate.looksLikeTemplate(trimmed)) {
                return QueryRef(null, null, null, QueryTemplate.parse(trimmed, label), label)
            }
            // 纯数字与布尔按字面量处理，避免被当成表达式重复求值。
            val numeric = trimmed.toDoubleOrNull()
            if (numeric != null) {
                return QueryRef(QueryText.parseScalar(trimmed), null, null, null, label, trimmed)
            }
            if (trimmed.equals("true", true) || trimmed.equals("false", true)) {
                return QueryRef(trimmed.equals("true", true), null, null, null, label, trimmed)
            }
            if (QueryPath.isPlainPath(trimmed)) {
                val root = trimmed.substringBefore('.')
                val resolved = QueryRoot.of(root)
                // 只写根名（如 locator="skill"）没有字段可取，按字面量处理；
                // sender / caster 允许单独使用，其余根必须带至少一个字段才是查询。
                if (resolved != null && (trimmed.contains('.') || resolved.bareAllowed)) {
                    // 保留原始文本：expr 这类条件需要整段公式，路径字段也要能回读原文。
                    return QueryRef(null, QueryPath.parse(trimmed, label), null, null, label, trimmed)
                }
            }
            val formula = tryExpression(trimmed, label)
            if (formula != null) {
                return QueryRef(null, null, formula, null, label, trimmed)
            }
            return QueryRef(QueryText.parseScalar(trimmed), null, null, null, label, trimmed)
        }

        /**
         * 尝试把文本解析为求值表达式。
         *
         * @param text 待解析文本。
         * @param label 配置路径，用于错误信息。
         * @return 表达式文本；解析失败时返回 null，由调用方按字面量处理。
         */
        private fun tryExpression(text: String, label: String): String? {
            try {
                validateExpression(text, label, false)
                return text
            } catch (exception: IllegalArgumentException) {
                return null
            }
        }

        /**
         * 按纯路径解析。
         *
         * 供模板分段复用，调用方已确认根合法。
         *
         * @param text 路径文本。
         * @param label 配置路径，用于错误信息。
         * @return 路径引用。
         */
        fun parsePath(text: String, label: String): QueryRef {
            return QueryRef(null, QueryPath.parse(text, label), null, null, label)
        }

        /**
         * 解析一段整段求值表达式。
         *
         * 用于 `expr{e="..."}` 这类「值本身就是表达式」的字段，路径裸写不包 `<>`。
         *
         * @param text 表达式文本。
         * @param label 配置路径，用于错误信息。
         * @return 已解析的取值引用。
         * @throws IllegalArgumentException 语法非法时抛出。
         */
        fun parseExpression(text: String, label: String): QueryRef {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("$label cannot be blank.")
            }
            validateExpression(trimmed, label, false)
            return QueryRef(null, null, trimmed, null, label, trimmed)
        }

        /**
         * 解析一段允许裸标识符的表达式。
         *
         * 裸标识符按查询上下文的变量表解析，用于等级表这类「同一条公式里
         * 直接引用 `level`」的配置；其他场景请用 [parseExpression]，
         * 以便未知标识符仍回退成字面量文本。
         *
         * @param text 表达式文本。
         * @param label 配置路径，用于错误信息。
         * @return 已解析的取值引用。
         * @throws IllegalArgumentException 语法非法时抛出。
         */
        fun parseVariableExpression(text: String, label: String): QueryRef {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("$label cannot be blank.")
            }
            validateExpression(trimmed, label, true)
            return QueryRef(null, null, trimmed, null, label, trimmed, true)
        }

        /**
         * 校验表达式语法与根合法性。
         *
         * 根校验复用 [QueryRoot]：允许变量的场景把未知裸标识符当作变量，其余场景报错，
         * 从而保留"未知标识符在加载期失败"的旧语义。
         *
         * @param text 表达式文本。
         * @param label 配置路径，用于错误信息。
         * @param allowVariables 是否允许裸标识符作为变量。
         * @throws IllegalArgumentException 语法或根非法时抛出。
         */
        private fun validateExpression(text: String, label: String, allowVariables: Boolean) {
            val compiled = QueryExpression.parse(text, label)
            for (root in compiled.roots()) {
                if (QueryRoot.of(root) != null) {
                    continue
                }
                if (allowVariables) {
                    continue
                }
                throw IllegalArgumentException("$label references unknown identifier '$root'.")
            }
        }

        /** 解析 `<>` 外壳内部的内容。 */
        private fun parseQuery(text: String, label: String): QueryRef {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("$label contains a blank query.")
            }
            if (QueryPath.isPlainPath(trimmed)) {
                val root = trimmed.substringBefore('.')
                val resolved = QueryRoot.of(root)
                // 只写根名（如 locator="skill"）没有字段可取，按字面量处理；
                // sender / caster 允许单独使用，其余根必须带至少一个字段才是查询。
                if (resolved != null && (trimmed.contains('.') || resolved.bareAllowed)) {
                    // 保留原始文本：expr 这类条件需要整段公式，路径字段也要能回读原文。
                    return QueryRef(null, QueryPath.parse(trimmed, label), null, null, label, trimmed)
                }
            }
            validateExpression(trimmed, label, false)
            return QueryRef(null, null, trimmed, null, label)
        }
    }
}
