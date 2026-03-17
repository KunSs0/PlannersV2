# 货币系统

货币配置文件位于 `plugins/Planners/module/currency/` 目录，用于对接经济插件（如 Vault）。

---

## 完整示例

以下是项目自带的 `module/currency/example.yml`：

```yaml
money:
  name: '金币'
  action:
    # 余额查询
    hook: getBalance()
    # 余额提取（扣钱）
    withdraw: takeMoney(arg)
    # 余额存入（加钱）
    deposit: giveMoney(arg)
    # 余额设置
    set: setMoney(arg)
```

一个文件中可以定义多个货币，每个货币以其 ID 作为 key（如 `money`）。

---

## 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 货币显示名称 |
| `action.hook` | String | 是 | 查询余额的 JS 表达式 |
| `action.withdraw` | String | 是 | 扣除货币的 JS 表达式 |
| `action.deposit` | String | 是 | 增加货币的 JS 表达式 |
| `action.set` | String | 是 | 设置余额的 JS 表达式 |

### 关于 `arg` 变量

在 `withdraw`、`deposit`、`set` 表达式中，`arg` 是系统自动注入的变量，代表操作的数量。

例如技能升级需要消耗 500 金币时，系统会调用 `withdraw` 表达式，此时 `arg = 500`。

---

## 在技能升级中使用

货币 ID 用在技能的 `upgrade.condition` 中：

```yaml
# skill/example.yml
upgrade:
  condition:
    0-100:
      money: 100.0 * level + 32.5    # "money" 对应货币 ID
```

系统会：
1. 调用 `hook`（`getBalance()`）查询玩家余额
2. 计算升级消耗（`100.0 * level + 32.5`）
3. 如果余额足够，调用 `withdraw`（`takeMoney(arg)`）扣除

---

## 自定义货币示例

### 使用元数据存储的自定义货币

```yaml
gem:
  name: '宝石'
  action:
    hook: hasMeta("currency.gem", sender) ? getMeta("currency.gem", sender) : 0
    withdraw: setMeta("currency.gem", getMeta("currency.gem", sender) - arg, sender)
    deposit: setMeta("currency.gem", (hasMeta("currency.gem", sender) ? getMeta("currency.gem", sender) : 0) + arg, sender)
    set: setMeta("currency.gem", arg, sender)
```

这个例子不依赖任何经济插件，直接用 Planners 的元数据系统存储货币。
