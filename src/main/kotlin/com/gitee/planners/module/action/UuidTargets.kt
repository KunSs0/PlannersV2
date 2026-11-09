package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import java.util.UUID
import taboolib.common.platform.function.warning

/**
 * 把逗号分隔的实体 UUID 还原成目标列表。
 *
 * 与 `finder.now` 配对使用：一次执行把当前选择存进标记，另一次执行再用本目标器还原。
 * 无法解析、已离线或已失效的项会被跳过并告警，不会让整行失败。
 */
object UuidTargets : ActionTargeter {

    override val id = "uuid"

    override val aliases = listOf("uuid")

    override val scope = ActionScope.SKILL

    override fun validate(arguments: ActionArguments) {
        arguments.require("value")
        arguments.rejectUnknown("value")
    }

    override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
        val text = arguments.requireText(scope, "value")
        if (text.isBlank()) {
            return emptyList()
        }
        val found = ArrayList<ActionTarget>()
        for (part in text.split(',')) {
            val token = part.trim()
            if (token.isEmpty()) {
                continue
            }
            val uuid = parseUuid(token, arguments.path)
            if (uuid == null) {
                continue
            }
            val entity = Bukkit.getEntity(uuid)
            if (entity == null) {
                continue
            }
            if (!entity.isValid) {
                continue
            }
            if (entity !is LivingEntity) {
                continue
            }
            found.add(ActionTarget.EntityTarget(entity))
        }
        return found
    }

    /**
     * 解析单个 UUID 文本。
     *
     * @param token 文本片段。
     * @param path 配置路径，用于告警。
     * @return UUID；格式非法时返回 null。
     */
    private fun parseUuid(token: String, path: String): UUID? {
        try {
            return UUID.fromString(token)
        } catch (exception: IllegalArgumentException) {
            warning("$path uuid 目标器跳过非法 UUID '$token'。")
            return null
        }
    }
}
