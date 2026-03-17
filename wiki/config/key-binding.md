# 按键绑定

按键绑定配置位于 `plugins/Planners/key-binding.yml`，定义可用的按键槽位。玩家可以在技能面板中将技能绑定到这些按键上。

---

## 完整示例

```yaml
# 按键 ID（唯一标识，不要重复）
key0:
  # 按键显示名称（在 UI 中显示给玩家看）
  name: R
  # 匹配规则：strict = 严格匹配，fuzzy = 模糊匹配
  matching-type: strict
  # 组合键输入超时时间（单位 tick，20 tick = 1 秒）
  # 对于单个按键无效，只在组合键时生效
  request-tick: 30
  # 实际映射的按键
  mapping: R
```

---

## 字段说明

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | String | 是 | - | 按键显示名称，在 UI 中展示 |
| `matching-type` | String | 否 | `strict` | 匹配模式（见下方说明） |
| `request-tick` | Int | 否 | `30` | 组合键输入超时时间（tick） |
| `mapping` | String | 是 | - | 按键映射字符串 |

---

## 匹配模式

### strict（严格匹配）

按键序列必须**完全一致**才会触发。

例如 `mapping: QQ`，玩家必须在 `request-tick` 时间内连续按两次 Q，中间不能按其他键。

### fuzzy（模糊匹配）

只要按键序列中**包含**目标序列就会触发。

例如 `mapping: QQ`，玩家按了 `Q R Q Q` 也会触发（因为包含了连续的 QQ）。

---

## 组合键

通过 `mapping` 定义多键组合：

```yaml
# 单键
key_r:
  name: R
  mapping: R

# 双键组合
key_qq:
  name: QQ
  matching-type: strict
  request-tick: 40
  mapping: QQ

# 三键组合
key_qrq:
  name: QRQ
  matching-type: strict
  request-tick: 60
  mapping: QRQ
```

**组合键的工作原理**：
1. 玩家按下第一个键（如 Q），开始计时
2. 在 `request-tick` 时间内，玩家需要按完剩余的键（如 R、Q）
3. 如果超时或按错键（strict 模式），组合键失败
4. 如果成功匹配，触发绑定的技能

---

## DragonCore 按键

如果服务器安装了 DragonCore 插件，可以使用客户端自定义按键：

```yaml
key_dc1:
  name: "技能1"
  matching-type: strict
  request-tick: 30
  mapping: KEY_1
```

DragonCore 的按键名称取决于客户端配置。

---

## Minecraft 原生交互

如果在 `config.yml` 中启用了交互动作：

```yaml
settings:
  minecraft:
    interaction-action:
      enable: true
```

玩家可以通过 Minecraft 原生操作（左键、右键、蹲下+左键等）触发技能，无需额外配置按键绑定。

---

## 玩家如何绑定技能

1. 输入 `/planners skill open` 打开技能面板
2. 点击想要绑定的技能
3. 在弹出的界面中选择要绑定的按键槽位
4. 绑定完成后，按对应按键即可释放技能

管理员也可以通过 API 代码设置绑定：

```kotlin
PlayerTemplateAPI.setSkillBinding(template, playerSkill, keyBinding)
```

---

## 多个按键配置示例

```yaml
key0:
  name: R
  matching-type: strict
  request-tick: 30
  mapping: R

key1:
  name: T
  matching-type: strict
  request-tick: 30
  mapping: T

key2:
  name: RR
  matching-type: strict
  request-tick: 40
  mapping: RR

key3:
  name: RT
  matching-type: strict
  request-tick: 40
  mapping: RT
```

这样玩家就有 4 个技能槽位：R、T、双击R、R+T 组合。
