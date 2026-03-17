# 冷却函数

用于管理技能冷却时间。

---

## getCooldown — 获取剩余冷却

```javascript
getCooldown(skill)                  // 获取 sender 的技能冷却
getCooldown(skill, player)          // 获取指定玩家的技能冷却
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `skill` | ImmutableSkill | 技能对象。在技能脚本中直接用 `skill` 变量 |
| `player` | Player | 可选。目标玩家 |

返回值：剩余冷却时间（tick）。如果没有冷却，返回 0。

```javascript
var remaining = getCooldown(skill)
tell("剩余冷却: " + remaining + " tick")
```

## setCooldown — 设置冷却

```javascript
setCooldown(skill, ticks)           // 设置 sender 的技能冷却
setCooldown(skill, ticks, player)   // 设置指定玩家的技能冷却
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `skill` | ImmutableSkill | 技能对象 |
| `ticks` | Int | 冷却时间（tick，20 tick = 1 秒） |
| `player` | Player | 可选。目标玩家 |

```javascript
// 设置当前技能冷却 5 秒
setCooldown(skill, 100)

// 设置冷却 10 秒
setCooldown(skill, 200)
```

## resetCooldown — 重置冷却

```javascript
resetCooldown(skill)                // 重置 sender 的技能冷却
resetCooldown(skill, player)        // 重置指定玩家的技能冷却
```

立即清除冷却，使技能可以再次使用。

```javascript
resetCooldown(skill)
tell("冷却已重置！")
```

## hasCooldown — 检查是否在冷却中

```javascript
hasCooldown(skill)                  // 检查 sender
hasCooldown(skill, player)          // 检查指定玩家
```

返回值：`true` = 在冷却中，`false` = 可以使用。

```javascript
if (hasCooldown(skill)) {
  tell("&c技能冷却中，请稍后再试！")
  return
}

// 释放技能...
setCooldown(skill, 100)
```

---

## 完整示例

```javascript
function main() {
  // 检查冷却
  if (hasCooldown(skill)) {
    var remaining = getCooldown(skill)
    tell("&c冷却中！剩余 " + Math.ceil(remaining / 20) + " 秒")
    return
  }

  // 释放技能
  var targets = finder().range(8).excludeType("PLAYER").limit(3).build()
  damage(50 + level * 20, targets)
  sound("ENTITY_PLAYER_ATTACK_SWEEP", 1.0, 1.0)

  // 设置冷却（随等级减少）
  var cd = Math.max(200 - level * 10, 60)
  setCooldown(skill, cd)

  tell("&a技能释放成功！冷却 " + Math.ceil(cd / 20) + " 秒")
}
```
