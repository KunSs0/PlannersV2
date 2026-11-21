package com.gitee.planners.core.skill.condition

import java.util.Locale

/**
 * `provider:itemId` 形式的物品引用。
 *
 * @property provider 提供者标识，小写。
 * @property itemId 提供者体系内的物品标识，保持原样。
 * @property raw 原始配置文本，用于回退文案。
 */
internal class ItemReference(
    val provider: String,
    val itemId: String,
    val raw: String,
) {

    companion object {

        /**
         * 解析配置里的物品引用。
         *
         * @param value 配置值。
         * @param path 配置路径，用于错误信息。
         * @return 已拆分的引用。
         * @throws IllegalArgumentException 缺少前缀、前缀或物品为空时抛出。
         */
        fun parse(value: Any?, path: String): ItemReference {
            val raw = value?.toString()?.trim() ?: throw IllegalArgumentException("$path 不能为空")
            if (raw.isEmpty()) {
                throw IllegalArgumentException("$path 不能为空")
            }
            val separator = raw.indexOf(':')
            if (separator <= 0 || separator == raw.length - 1) {
                throw IllegalArgumentException("$path 必须写成 provider:itemId，实际：$raw")
            }
            val provider = raw.substring(0, separator).trim().lowercase(Locale.ROOT)
            val itemId = raw.substring(separator + 1).trim()
            require(provider.isNotEmpty()) { "$path 的 provider 不能为空：$raw" }
            require(itemId.isNotEmpty()) { "$path 的 itemId 不能为空：$raw" }
            return ItemReference(provider, itemId, raw)
        }
    }
}
