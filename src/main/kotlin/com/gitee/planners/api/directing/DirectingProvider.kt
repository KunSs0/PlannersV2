package com.gitee.planners.api.directing

/**
 * 指向性技能 provider。
 *
 * provider 负责解析其专属配置，并在后续运行时负责对应的输入、瞄准与确认实现。
 * type 是全局唯一标识，例如 `fc.entity`、`fc.position` 或 `fc.direction`。
 */
interface DirectingProvider {

    /**
     * provider 的全局唯一类型标识。
     */
    val type: String

    /**
     * 解析技能 `__option__.directing` 配置段。
     *
     * @param section 完整的 directing 配置段。
     * @return 不可变的指向性技能定义。
     */
    fun decode(config: DirectingConfig): DirectingDefinition
}
