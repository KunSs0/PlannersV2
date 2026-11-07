package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import java.util.Locale

/**
 * 动作、条件、目标器与触发器的参数表。
 *
 * 参数在加载期完成拆分、去引号与取值引用解析，运行期只按键取值。
 * 同一参数可以声明多个别名；键不区分大小写。
 *
 * @property path 当前配置路径，用于错误信息。
 */
class ActionArguments(values: Map<String, String>, val path: String) {

    private val refs: Map<String, QueryRef>

    /** 全部参数键，按声明顺序。 */
    val keys: Set<String>
        get() {
            return refs.keys
        }

    init {
        val snapshot = LinkedHashMap<String, QueryRef>()
        for (entry in values.entries) {
            val key = entry.key.trim().lowercase(Locale.ROOT)
            if (key.isEmpty()) {
                throw IllegalArgumentException("$path contains a blank argument name.")
            }
            if (snapshot.containsKey(key)) {
                throw IllegalArgumentException("$path contains duplicate argument '$key'.")
            }
            val label = "$path.$key"
            snapshot[key] = QueryRef.parse(entry.value, label)
        }
        this.refs = snapshot
    }

    /**
     * 按键与别名查找参数引用。
     *
     * @param keys 依次尝试的参数名。
     * @return 命中的引用；未命中时返回 null。
     */
    fun get(vararg keys: String): QueryRef? {
        for (key in keys) {
            val value = refs[key.lowercase(Locale.ROOT)]
            if (value != null) {
                return value
            }
        }
        return null
    }

    /**
     * 读取必填参数引用。
     *
     * @param keys 依次尝试的参数名。
     * @return 参数引用。
     * @throws IllegalArgumentException 参数缺失时抛出。
     */
    fun require(vararg keys: String): QueryRef {
        val value = get(*keys)
        if (value == null) {
            throw IllegalArgumentException("$path requires argument '${keys.first()}'.")
        }
        return value
    }

    /**
     * 读取可选文本参数。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 文本值；未配置时返回 null。
     */
    fun text(scope: QueryScope, vararg keys: String): String? {
        val value = get(*keys)
        if (value == null) {
            return null
        }
        return value.resolveText(scope)
    }

    /**
     * 读取必填文本参数。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 文本值。
     * @throws IllegalArgumentException 参数缺失或为空时抛出。
     */
    fun requireText(scope: QueryScope, vararg keys: String): String {
        val value = require(*keys).resolveText(scope)
        if (value.isBlank()) {
            throw IllegalArgumentException("$path argument '${keys.first()}' cannot be blank.")
        }
        return value
    }

    /**
     * 读取可选数值参数。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 数值；未配置时返回 null。
     */
    fun number(scope: QueryScope, vararg keys: String): Double? {
        val value = get(*keys)
        if (value == null) {
            return null
        }
        return value.resolveDouble(scope)
    }

    /**
     * 读取必填数值参数。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 数值。
     * @throws IllegalArgumentException 参数缺失时抛出。
     */
    fun requireNumber(scope: QueryScope, vararg keys: String): Double {
        return require(*keys).resolveDouble(scope)
    }

    /**
     * 读取可选整数参数。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 整数；未配置时返回 null。
     */
    fun integer(scope: QueryScope, vararg keys: String): Int? {
        val value = number(scope, *keys)
        if (value == null) {
            return null
        }
        return Math.round(value).toInt()
    }

    /**
     * 读取必填整数参数。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 整数。
     * @throws IllegalArgumentException 参数缺失时抛出。
     */
    fun requireInteger(scope: QueryScope, vararg keys: String): Int {
        val value = Math.round(requireNumber(scope, *keys))
        return value.toInt()
    }

    /**
     * 读取可选布尔参数。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 布尔；未配置时返回 null。
     */
    fun boolean(scope: QueryScope, vararg keys: String): Boolean? {
        val value = get(*keys)
        if (value == null) {
            return null
        }
        return value.resolveBoolean(scope)
    }

    /**
     * 读取逗号分隔的文本列表。
     *
     * @param keys 依次尝试的参数名。
     * @param scope 查询上下文。
     * @return 去除空白后的非空项列表。
     */
    fun list(scope: QueryScope, vararg keys: String): List<String> {
        val raw = text(scope, *keys)
        if (raw == null) {
            return emptyList()
        }
        val result = ArrayList<String>()
        for (part in raw.split(',')) {
            val item = part.trim()
            if (item.isNotEmpty()) {
                result.add(item)
            }
        }
        return result
    }

    /**
     * 读取参数的原始文本，不做任何求值。
     *
     * 供「值本身就是公式」的字段使用，例如条件 `expr` 的整段表达式。
     *
     * @param keys 依次尝试的参数名。
     * @return 原始文本；未配置时返回 null。
     */
    fun raw(vararg keys: String): String? {
        val ref = get(*keys)
        if (ref == null) {
            return null
        }
        return ref.sourceText()
    }

    /**
     * 读取字面量整数参数，用于加载期读取静态配置。
     *
     * @param keys 依次尝试的参数名。
     * @return 字面量整数；未配置或不是字面量时返回 null。
     * @throws IllegalArgumentException 字面量不是整数时抛出。
     */
    fun literalInteger(vararg keys: String): Int? {
        val ref = get(*keys)
        if (ref == null) {
            return null
        }
        if (!ref.isLiteral()) {
            return null
        }
        val value = ref.literalValue()
        if (value is Number) {
            return Math.round(value.toDouble()).toInt()
        }
        if (value is String) {
            val parsed = value.trim().toIntOrNull()
            if (parsed == null) {
                throw IllegalArgumentException("$path argument '${keys.first()}' must be an integer.")
            }
            return parsed
        }
        throw IllegalArgumentException("$path argument '${keys.first()}' must be an integer.")
    }

    /**
     * 读取字面量文本参数，用于加载期读取静态配置。
     *
     * @param keys 依次尝试的参数名。
     * @return 字面量文本；未配置或不是字面量时返回 null。
     */
    fun literalText(vararg keys: String): String? {
        val ref = get(*keys)
        if (ref == null) {
            return null
        }
        if (!ref.isLiteral()) {
            return null
        }
        val value = ref.literalValue()
        if (value == null) {
            return null
        }
        return value.toString()
    }

    /**
     * 判断参数是否为动态值（`<>` 查询或求值表达式）。
     *
     * @param keys 依次尝试的参数名。
     * @return 存在且为动态值时返回 true。
     */
    fun isDynamic(vararg keys: String): Boolean {
        val ref = get(*keys)
        if (ref == null) {
            return false
        }
        return !ref.isLiteral()
    }

    /**
     * 判断是否存在指定参数。
     *
     * @param key 参数名。
     * @return 存在时返回 true。
     */
    fun contains(key: String): Boolean {
        return refs.containsKey(key.lowercase(Locale.ROOT))
    }

    /**
     * 返回全部参数引用，用于诊断输出。
     *
     * @return 键到引用的只读视图。
     */
    fun all(): Map<String, QueryRef> {
        return refs
    }

    /**
     * 校验参数中没有未知键。
     *
     * @param allowed 允许的键集合，大小写不敏感。
     * @throws IllegalArgumentException 出现未知键时抛出。
     */
    fun rejectUnknown(vararg allowed: String) {
        val permitted = LinkedHashSet<String>()
        for (key in allowed) {
            permitted.add(key.lowercase(Locale.ROOT))
        }
        for (key in refs.keys) {
            if (!permitted.contains(key)) {
                throw IllegalArgumentException("$path contains unsupported argument '$key'.")
            }
        }
    }
}
