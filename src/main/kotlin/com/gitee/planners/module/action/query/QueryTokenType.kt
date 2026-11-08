package com.gitee.planners.module.action.query

/**
 * 求值表达式的词法单元类型。
 *
 * 不包含函数调用：标识符之后即使紧跟 `(`，也按求值表达式里不支持函数报错。
 */
enum class QueryTokenType {

    /** 数字字面量。 */
    NUMBER,

    /** 引号字符串。 */
    STRING,

    /** 标识符或路径，例如 `skill.level`、`attr.STR`。 */
    IDENTIFIER,

    /** `+` */
    PLUS,

    /** `-` */
    MINUS,

    /** `*` */
    STAR,

    /** `/` */
    SLASH,

    /** `%` */
    PERCENT,

    /** `==` */
    EQUAL,

    /** `!=` */
    NOT_EQUAL,

    /** `<` */
    LESS,

    /** `<=` */
    LESS_EQUAL,

    /** `>` */
    GREATER,

    /** `>=` */
    GREATER_EQUAL,

    /** `&&` */
    AND,

    /** `||` */
    OR,

    /** `!` */
    NOT,

    /** `?` */
    QUESTION,

    /** `:` */
    COLON,

    /** `(` */
    LEFT_PAREN,

    /** `)` */
    RIGHT_PAREN,

    /** `[`，用于列表下标取值。 */
    LEFT_BRACKET,

    /** `]` */
    RIGHT_BRACKET,

    /** `,`，用于列表字面量。 */
    COMMA,

    /** 结束标记。 */
    END,
}
