package com.gitee.planners.core.value

/**
 * 单个命名修正。
 *
 * @property id 配置中声明的修正 ID。
 * @property condition 生效条件；为空表示始终生效。
 * @property operation 修正表达式，例如 `+1`、`*2`。
 */
data class IntValueModifier(
    val id: String,
    val condition: LevelCondition?,
    val operation: String,
)
