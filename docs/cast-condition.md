# 技能释放条件

技能可以在 `__option__.condition.consume.cast` 下声明第三方释放条件。条件实现由外部插件通过 `PlannersAPI.registerCastCondition` 注册，技能配置只负责引用条件 ID 和传入参数。

```yaml
__option__:
  condition:
    consume:
      cast:
        item_holy_emblem:
          props:
            item_id: holy_emblem
            amount: 1
          cost:
            timing: COMMIT

        fightcore_player_state:
          props:
            required: combat
```

条件 ID 使用单层配置键，只允许字母、数字和下划线，禁止 `.`。例如 `fightcore_player_state`。`props` 的内容原样传给第三方条件实现。

消耗时机目前有两种：

| 时机 | 行为 |
| --- | --- |
| `START` | 初始校验通过后消耗，适合进入技能流程就必须支付的资源 |
| `COMMIT` | Hook、读条或指向确认完成，最终校验通过后消耗 |

技能释放时会先进行一次条件校验；进入最终执行阶段前会再次校验。`COMMIT` 条件适合读条技能：读条被取消时不会消耗资源，完成后才会提交消耗。

第三方插件注册条件：

```kotlin
PlannersAPI.registerCastCondition("fightcore_player_state", condition)
```

当前实现仍有以下限制：多个条件的消耗还没有统一事务回滚；`resume()` 的一次性提交保护尚未完成；OR 条件组和技能执行成功后的结果条件不属于本版本配置范围。
