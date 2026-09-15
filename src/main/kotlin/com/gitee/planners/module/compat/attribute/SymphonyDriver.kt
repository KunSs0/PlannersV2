package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.common.task.SimpleUniqueTask
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.RegisteredServiceProvider
import priv.seventeen.artist.symphony.api.attribute.AttributeKey
import priv.seventeen.artist.symphony.api.service.SymphonyApi
import taboolib.common.platform.function.warning
import taboolib.common5.clong
import java.util.Locale

/**
 * Symphony 属性插件驱动。
 *
 * 该驱动通过 Bukkit 服务管理器获取 Symphony API，并将 Planners 的属性源
 * 文本转换为 Symphony 的 `namespace:attribute operation value` 格式。
 */
object SymphonyDriver : AttributeDriver {

    private const val PLUGIN_NAME = "Symphony"
    private const val SOURCE_NAMESPACE = "planners"
    private val whitespacePattern = Regex("\\s+")
    private val qualifiedAttributePattern = Regex("^[a-z0-9._-]+:[a-z0-9._/-]+$")
    private val bareAttributePattern = Regex("^[a-z0-9._/-]+$")

    /**
     * 检查 Symphony 插件和公开 API 服务是否已经启用。
     *
     * @return Symphony 插件可用时返回 true。
     */
    override fun checkEnable(): Boolean {
        val plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME)
        if (plugin == null) {
            return false
        }
        if (!plugin.isEnabled) {
            return false
        }
        return getApi() != null
    }

    /**
     * 设置目标实体的 Symphony 属性源。
     *
     * @param target 目标代理。
     * @param id 属性源标识。
     * @param source Planners 属性源文本。
     * @param timeout 超时时间，单位为 tick，-1 表示不超时。
     */
    override fun set(target: ProxyTarget<*>, id: String, source: List<String>, timeout: Int) {
        if (target !is ProxyTarget.BukkitEntity) {
            warning("Target type must be ProxyTarget.BukkitEntity.")
            return
        }
        val entity = target.instance
        if (entity !is LivingEntity) {
            warning("Symphony 属性只能应用于 LivingEntity。")
            return
        }
        val api = getApi()
        if (api == null) {
            warning("Symphony API 服务不可用，无法设置属性源。")
            return
        }
        val lines = normalizeSourceLines(source)
        if (lines.isEmpty()) {
            remove(target, id)
            return
        }
        val key = createSourceKey(id)
        // 通过 Symphony 来源服务替换同一来源，触发属性重算。
        api.sources.replaceSourceFromLines(entity, key, lines)
        if (timeout != -1) {
            val uniqueId = "symphony.${entity.uniqueId}.$id"
            // 使用 Planners 的唯一任务保持原有 tick 超时语义。
            SimpleUniqueTask.create(uniqueId, timeout.clong, true) {
                remove(target, id)
            }
        }
    }

    /**
     * 设置 Bukkit 实体的 Symphony 属性源。
     *
     * @param entity Bukkit 实体。
     * @param id 属性源标识。
     * @param source Planners 属性源文本。
     * @param timeout 超时时间，单位为 tick，-1 表示不超时。
     */
    override fun set(entity: Entity, id: String, source: List<String>, timeout: Int) {
        set(entity.asTarget(), id, source, timeout)
    }

    /**
     * 移除目标实体的 Symphony 属性源。
     *
     * @param target 目标代理。
     * @param id 属性源标识。
     */
    override fun remove(target: ProxyTarget<*>, id: String) {
        if (target !is ProxyTarget.BukkitEntity) {
            warning("Target type must be ProxyTarget.BukkitEntity.")
            return
        }
        val entity = target.instance
        if (entity !is LivingEntity) {
            warning("Symphony 属性只能应用于 LivingEntity。")
            return
        }
        val api = getApi()
        if (api == null) {
            warning("Symphony API 服务不可用，无法移除属性源。")
            return
        }
        // 通过相同来源键移除整组属性修饰。
        api.sources.removeSource(entity, createSourceKey(id))
    }

    /**
     * 读取实体的最终 Symphony 属性值。
     *
     * @param entity Bukkit 实体。
     * @param attrName 属性名，可使用完整 `namespace:name` 或裸属性名。
     * @return 属性值列表；实体、API 或属性不存在时返回空列表。
     */
    override fun get(entity: Entity, attrName: String): List<Double> {
        if (entity !is LivingEntity) {
            return emptyList()
        }
        val api = getApi()
        if (api == null) {
            return emptyList()
        }
        val keyText = normalizeAttributeKey(attrName)
        if (keyText == null) {
            return emptyList()
        }
        val key = AttributeKey(keyText)
        // 从 Symphony 快照读取最终聚合值，与 AttributeDriver 的读取约定保持一致。
        val values = api.attributes.snapshot(entity).values
        val value = values[key]
        if (value == null) {
            return emptyList()
        }
        return listOf(value)
    }

    /**
     * 从 Bukkit 服务管理器获取 Symphony API。
     *
     * @return 已注册的 API，未注册时返回 null。
     */
    private fun getApi(): SymphonyApi? {
        val registration: RegisteredServiceProvider<SymphonyApi>? =
            Bukkit.getServicesManager().getRegistration(SymphonyApi::class.java)
        if (registration == null) {
            return null
        }
        return registration.provider
    }

    /**
     * 创建 Planners 专用属性源键。
     *
     * @param id Planners 属性源标识。
     * @return Symphony 属性源键。
     */
    private fun createSourceKey(id: String): priv.seventeen.artist.symphony.api.attribute.AttributeSourceKey {
        return priv.seventeen.artist.symphony.api.attribute.AttributeSourceKey(SOURCE_NAMESPACE, id)
    }

    /**
     * 转换整组属性源文本，并丢弃空行、注释和无效属性键。
     *
     * @param source Planners 属性源文本。
     * @return Symphony 属性源文本。
     */
    private fun normalizeSourceLines(source: List<String>): List<String> {
        val result = ArrayList<String>()
        for (line in source) {
            val normalized = normalizeSourceLine(line)
            if (normalized != null) {
                result.add(normalized)
            }
        }
        return result
    }

    /**
     * 将单行 Planners 属性源转换为 Symphony 格式。
     *
     * @param line Planners 属性源单行文本。
     * @return Symphony 属性源单行文本，无效行返回 null。
     */
    private fun normalizeSourceLine(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null
        }
        val tokens = trimmed.split(whitespacePattern, limit = 3)
        if (tokens.size == 2 && tokens[0].endsWith(":")) {
            val key = normalizeAttributeKey(tokens[0].dropLast(1))
            if (key == null) {
                warning("Symphony 忽略无效属性键: ${tokens[0]}")
                return null
            }
            return "$key add ${stripPositiveSign(tokens[1])}"
        }
        if (tokens.size == 3 && tokens[0].endsWith(":")) {
            val key = normalizeAttributeKey(tokens[0].dropLast(1))
            if (key == null) {
                warning("Symphony 忽略无效属性键: ${tokens[0]}")
                return null
            }
            if (tokens[1] == "-") {
                return "$key add -${tokens[2]}"
            }
            val operation = normalizeOperation(tokens[1])
            if (operation == null) {
                warning("Symphony 忽略无效属性操作: ${tokens[1]}")
                return null
            }
            return "$key $operation ${tokens[2]}"
        }
        if (tokens.size != 3) {
            warning("Symphony 忽略格式错误的属性源: $line")
            return null
        }
        val key = normalizeAttributeKey(tokens[0])
        if (key == null) {
            warning("Symphony 忽略无效属性键: ${tokens[0]}")
            return null
        }
        val operation = normalizeOperation(tokens[1])
        if (operation == null) {
            warning("Symphony 忽略无效属性操作: ${tokens[1]}")
            return null
        }
        return "$key $operation ${tokens[2]}"
    }

    /**
     * 规范化 Symphony 属性键。
     *
     * @param raw 原始属性键。
     * @return 合法的完整属性键，无效键返回 null。
     */
    private fun normalizeAttributeKey(raw: String): String? {
        val key = raw.trim().lowercase(Locale.ROOT)
        if (qualifiedAttributePattern.matches(key)) {
            return key
        }
        if (bareAttributePattern.matches(key)) {
            return "symphony:$key"
        }
        return null
    }

    /**
     * 将 Planners 常见操作符转换为 Symphony 操作名。
     *
     * @param raw 原始操作符。
     * @return Symphony 操作名，无效操作返回 null。
     */
    private fun normalizeOperation(raw: String): String? {
        val operation = raw.lowercase(Locale.ROOT)
        if (operation == "+" || operation == "add") {
            return "add"
        }
        if (operation == "multiply_base" || operation == "multiply-base" || operation == "*base") {
            return "multiply_base"
        }
        if (operation == "multiply_total" || operation == "multiply-total" || operation == "*total") {
            return "multiply_total"
        }
        return null
    }

    /**
     * 删除加法数值中的正号，避免与操作符重复表达。
     *
     * @param value 原始数值。
     * @return 去除正号后的数值。
     */
    private fun stripPositiveSign(value: String): String {
        if (value.startsWith("+")) {
            return value.drop(1)
        }
        return value
    }
}
