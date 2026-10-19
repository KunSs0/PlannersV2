# 配置变量：常量与显式表达式

从 3.7.0 起，技能和职业 `__option__.variables` 的普通值就是配置数据。
字符串不执行脚本、不进行字符串插值，也不自动转换为数字。

```yaml
variables:
  hitType: hurt1
  hitAttackLevel: 1
  enabled: true
  numericText: '0'
  offsets: [1, 2, 3]
  power:
    expression: 70 + 20 * level
  doubled:
    expression: power * 2
```

`expression` 必须是唯一字段，值必须是非空 Nova 表达式字符串。
表达式按配置声明顺序计算，可以引用此前定义的常量或计算结果。
普通值由宿主绑定传入，计算结果与常量共同写入本次技能上下文；
动作中的 `this.hitType`、`this.power` 只读取这些值，不执行二次求值。
列表常量在每次求值时复制，避免一次施法中的修改污染后续施法。

原有 `condition` / `action` 条件变量仍使用这两个显式表达式字段。
其他语义明确的表达式字段（例如职业 `hook.attributes`、模板 `{{...}}`）不受此次调整影响。
变量常量字符串里的 `{{...}}` 也只是文本，不登记为模板源码。

升级时必须同步迁移配置：原本作为公式填写的字符串改为 `expression` 结构；
带双层引号的字符串常量改为普通 YAML 字符串；原数字表达式 `'0'` 改为数字 `0`。
没有旧格式识别、自动猜测或求值失败后转字符串的兼容处理。
