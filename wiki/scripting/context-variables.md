# 上下文变量

在脚本执行时，Planners 会自动注入一些变量到脚本环境中。这些变量可以直接使用，不需要声明。

---

## 技能脚本中的变量

当技能释放时，以下变量自动可用：

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `sender` | Player / Entity | 技能释放者。通常是玩家，也可以是其他实体 |
| `level` | Int | 技能当前等级 |
| `skill` | ImmutableSkill | 当前技能对象。主要用于 `setCooldown(skill, ticks)` |
| `ctx` | SkillContext | 技能上下文对象 |
| `profile` | PlayerTemplate | 玩家档案。仅当 sender 是玩家时可用 |
| `origin` | Location | 技能释放时的位置 |

### sender

`sender` 是技能的释放者，通常是一个 Player 对象。

```javascript
function main() {
  // 获取释放者名称
  tell("释放者: " + sender.getName())

  // 获取释放者位置
  var loc = sender.getLocation()
  tell("位置: " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ())

  // 获取释放者生命值
  tell("生命值: " + sender.getHealth())
}
```

### level

`level` 是技能的当前等级，是一个整数。

```javascript
function main() {
  var damage = 50 + level * 20
  tell("等级 " + level + " 的伤害: " + damage)
}
```

### skill

`skill` 是当前技能对象，主要用于冷却函数：

```javascript
function main() {
  // 设置当前技能冷却 200 tick
  setCooldown(skill, 200)

  // 检查当前技能是否在冷却中
  if (hasCooldown(skill)) {
    tell("技能冷却中！")
    return
  }
}
```

### profile

`profile` 是玩家的档案对象，包含等级、经验、法力值等信息。

```javascript
function main() {
  // 获取玩家等级
  tell("玩家等级: " + profile.getLevel())

  // 获取玩家法力值
  tell("法力值: " + profile.getMagicPoint())
}
```

> **注意**：只有当 sender 是玩家时 `profile` 才可用。如果是其他实体释放技能，`profile` 为 null。

### origin

`origin` 是技能释放时释放者的位置（Bukkit Location 对象）。

```javascript
function main() {
  // 在释放位置生成一个僵尸
  entitySpawn("ZOMBIE", 100)  // 默认在 sender 位置生成
}
```

---

## 变量表达式中的变量

在技能的 `variables` 和图标 `{{}}` 表达式中，可用的变量：

| 变量名 | 说明 |
|--------|------|
| `level` | 技能等级 |

```yaml
variables:
  damage: 32 * level + 100      # level 是技能等级
  cooldown: 200 - level * 10
```

---

## 等级算法中的变量

在 `module/level/*.yml` 的 `experience` 表达式中：

| 变量名 | 说明 |
|--------|------|
| `level` | 当前等级 |

```yaml
experience: |
  level <= 10 ? level * 200 : level * 1000
```

---

## 法力值公式中的变量

在 `config.yml` 的法力值表达式中：

| 变量名 | 说明 |
|--------|------|
| `profile.level` | 玩家等级 |
| `profile.magicPoint` | 当前法力值 |

```yaml
upper-limit:
  expression: "profile.level * 2 + 100"
```

---

## 货币表达式中的变量

在 `module/currency/*.yml` 的 action 表达式中：

| 变量名 | 说明 |
|--------|------|
| `arg` | 操作数量（扣除/增加/设置的金额） |
| `sender` | 操作目标玩家 |

```yaml
action:
  withdraw: takeMoney(arg)
```
