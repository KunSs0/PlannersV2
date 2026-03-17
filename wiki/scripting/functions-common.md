# 通用函数

## tell — 发送消息

向目标发送聊天消息。

```javascript
tell(message)               // 发送消息给 sender
tell(message, targets)      // 发送消息给指定目标
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `message` | String | 消息内容，支持 `&` 颜色代码 |
| `targets` | ProxyTargetContainer | 可选。消息接收者，不传则发给 sender |

### 示例

```javascript
// 给释放者发消息
tell("技能释放成功！")

// 带颜色代码
tell("&a技能释放成功！&7冷却 5 秒")

// 给目标发消息
var targets = finder().range(10).type("PLAYER").build()
tell("&c你被攻击了！", targets)
```
