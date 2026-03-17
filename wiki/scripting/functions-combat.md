# 战斗函数

包含伤害、治疗、生命值、速度、药水效果、冻结、点燃、爆炸等战斗相关函数。

---

## 伤害函数

### damage — 造成伤害

```javascript
damage(amount)                          // 对 sender 造成伤害（伤害类型: SKILL）
damage(amount, targets)                 // 对目标造成伤害
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `amount` | Double | 伤害值 |
| `targets` | ProxyTargetContainer | 可选。目标，不传则作用于 sender |

```javascript
// 对自己造成 10 点伤害
damage(10)

// 对目标造成伤害
var targets = finder().range(5).type("ZOMBIE").build()
damage(50, targets)
```

### damageBy — 指定来源伤害

```javascript
damageBy(amount, source)                // 指定伤害来源
damageBy(amount, source, targets)       // 指定来源和目标
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `amount` | Double | 伤害值 |
| `source` | LivingEntity | 伤害来源实体 |
| `targets` | ProxyTargetContainer | 可选。目标 |

```javascript
// sender 对目标造成 50 点伤害（会触发攻击事件）
damageBy(50, sender, targets)
```

### damageEx — 指定伤害类型

```javascript
damageEx(amount, cause)                 // 指定伤害类型
damageEx(amount, cause, targets)        // 指定类型和目标
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `amount` | Double | 伤害值 |
| `cause` | String | 伤害类型名称（需在 config.yml 的 `damage-causes` 中注册） |
| `targets` | ProxyTargetContainer | 可选。目标 |

```javascript
damageEx(30, "SKILL", targets)
damageEx(50, "ATTRIBUTE", targets)
```

### damageExBy — 完整伤害

```javascript
damageExBy(amount, cause, source)               // 完整参数
damageExBy(amount, cause, source, targets)      // 完整参数 + 目标
```

```javascript
damageExBy(100, "SKILL", sender, targets)
```

### heal — 治疗

```javascript
heal(amount)                            // 治疗 sender
heal(amount, targets)                   // 治疗目标
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `amount` | Double | 治疗量。不会超过最大生命值 |

```javascript
heal(20)                                // 治疗自己 20 点
heal(50, targets)                       // 治疗目标 50 点
```

---

## 生命值函数

### healthAdd — 增加生命值

```javascript
healthAdd(amount)
healthAdd(amount, targets)
```

### healthSet — 设置生命值

```javascript
healthSet(amount)
healthSet(amount, targets)
```

### healthTake — 扣除生命值

```javascript
healthTake(amount)
healthTake(amount, targets)
```

```javascript
// 给自己加 10 点血
healthAdd(10)

// 设置目标生命值为 1
healthSet(1, targets)

// 扣除目标 5 点血（不触发伤害事件）
healthTake(5, targets)
```

> **heal vs healthAdd 的区别**：`heal` 不会超过最大生命值，`healthAdd` 也不会超过最大生命值。但 `healthTake` 不会触发伤害事件，而 `damage` 会。

---

## 速度函数

### velocitySet — 设置速度

```javascript
velocitySet(x, y, z)                    // 设置 sender 速度
velocitySet(x, y, z, targets)           // 设置目标速度
```

### velocityAdd — 叠加速度

```javascript
velocityAdd(x, y, z)
velocityAdd(x, y, z, targets)
```

### velocityMove — 相对朝向移动

```javascript
velocityMove(x, y, z)
velocityMove(x, y, z, targets)
```

速度方向相对于实体的朝向。x = 左右，y = 上下，z = 前后。

### velocityZero — 清零速度

```javascript
velocityZero()
velocityZero(targets)
```

### getVelocity — 获取速度

```javascript
var vel = getVelocity(entity)
```

返回 Bukkit Vector 对象。

```javascript
// 把目标弹飞
velocityAdd(0, 1.5, 0, targets)

// 向前冲刺
velocityMove(0, 0.3, 2.0)

// 停止移动
velocityZero(targets)
```

---

## 药水效果

### potion — 添加药水效果

```javascript
potion(type, level, duration)
potion(type, level, duration, targets)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | String | 药水类型（Bukkit PotionEffectType 名称） |
| `level` | Int | 效果等级（0 = 一级，1 = 二级） |
| `duration` | Int | 持续时间（tick，20 tick = 1 秒） |

常用药水类型：

| 类型 | 说明 |
|------|------|
| `SPEED` | 速度 |
| `SLOW` | 缓慢 |
| `FAST_DIGGING` | 急迫 |
| `SLOW_DIGGING` | 挖掘疲劳 |
| `INCREASE_DAMAGE` | 力量 |
| `HEAL` | 瞬间治疗 |
| `HARM` | 瞬间伤害 |
| `JUMP` | 跳跃提升 |
| `CONFUSION` | 反胃 |
| `REGENERATION` | 生命恢复 |
| `DAMAGE_RESISTANCE` | 抗性提升 |
| `FIRE_RESISTANCE` | 防火 |
| `WATER_BREATHING` | 水下呼吸 |
| `INVISIBILITY` | 隐身 |
| `BLINDNESS` | 失明 |
| `POISON` | 中毒 |
| `WITHER` | 凋零 |
| `GLOWING` | 发光 |
| `LEVITATION` | 漂浮 |

### potionRemove — 移除药水效果

```javascript
potionRemove(type)
potionRemove(type, targets)
```

```javascript
// 给自己加速 10 秒
potion("SPEED", 1, 200)

// 给目标施加缓慢 3 秒
potion("SLOW", 2, 60, targets)

// 移除自己的缓慢效果
potionRemove("SLOW")
```

---

## 效果函数

### freeze — 冻结

```javascript
freeze(ticks)
freeze(ticks, targets)
```

使实体进入冻结状态（类似细雪效果）。

### fire — 点燃

```javascript
fire(ticks)
fire(ticks, targets)
```

使实体着火。

### explosion — 爆炸

```javascript
explosion(power)
explosion(power, fire, breakBlocks, targets)
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `power` | Float | - | 爆炸威力 |
| `fire` | Boolean | `false` | 是否产生火焰 |
| `breakBlocks` | Boolean | `false` | 是否破坏方块 |

```javascript
// 在目标位置产生爆炸（不破坏方块）
explosion(4.0, false, false, targets)

// 冻结目标 5 秒
freeze(100, targets)

// 点燃目标 3 秒
fire(60, targets)
```
