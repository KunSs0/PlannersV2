# JS API 完整参考

## 全局可用变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `sender` | ProxyTarget | 施法者（通常为 Player） |
| `origin` | Location | 施法原点位置 |
| `level` | int | 技能等级 |
| `skill` | ImmutableSkill | 技能定义对象 |
| `profile` | PlayerTemplate | 玩家完整数据（仅 sender 为 Player） |
| `ctx` | SkillContext | 完整执行上下文 |
| `target` | ProxyTarget | 目标实体（由脚本流程动态设置） |
| `event` | Event | 状态回调时的事件对象 |

### skill 对象属性

```
skill.id          — 技能 ID（文件名不含扩展名）
skill.name        — 技能名称
skill.getVariable("key")  — 获取变量值
```

---

## 通用

| 函数 | 说明 |
|------|------|
| `random(min, max)` | 随机整数 [min, max] |
| `sleep(ms)` | 暂停毫秒（仅 async=true 可用） |
| `tell(msg)` / `tell(msg, targets)` | 发送消息 |

---

## 伤害与治疗

| 函数 | 说明 |
|------|------|
| `damage(amount)` / `damage(amount, targets)` | 技能伤害（cause=SKILL） |
| `damageBy(amount, source)` / `damageBy(amount, source, targets)` | 指定来源伤害 |
| `damageEx(amount, cause)` / `damageEx(amount, cause, targets)` | 自定义 cause 伤害 |
| `damageExBy(amount, cause, source)` | 完整自定义伤害 |
| `heal(amount)` / `heal(amount, targets)` | 治疗 |

---

## 冷却

| 函数 | 说明 |
|------|------|
| `getCooldown(skill)` / `getCooldown(skill, player)` | 获取剩余冷却(ticks) |
| `setCooldown(skill, ticks)` / `setCooldown(skill, ticks, player)` | 设置冷却 |
| `resetCooldown(skill)` / `resetCooldown(skill, player)` | 重置冷却为 0 |
| `hasCooldown(skill)` / `hasCooldown(skill, player)` | 是否在冷却中 |

`skill` 参数可以是技能 ID 字符串或 `skill` 对象。

---

## 生命值

| 函数 | 说明 |
|------|------|
| `healthAdd(amount)` / `healthAdd(amount, targets)` | 增加生命（不超过上限） |
| `healthSet(amount)` / `healthSet(amount, targets)` | 直接设置生命 |
| `healthTake(amount)` / `healthTake(amount, targets)` | 扣除生命（不低于 0） |

---

## 实体操作

| 函数 | 说明 |
|------|------|
| `entitySpawn(type)` / `entitySpawn(type, duration)` / `entitySpawn(type, duration, locations)` | 生成实体，duration 后移除 |
| `entityRemove()` / `entityRemove(targets)` | 移除实体 |
| `entityTeleport(x, y, z)` / `entityTeleport(x, y, z, targets)` | 传送 |
| `entityTeleportTo(destinations)` / `entityTeleportTo(targets, destinations)` | 传送到目标 |
| `entitySetAI(bool)` / `entitySetAI(bool, targets)` | AI 开关 |
| `entitySetGravity(bool)` / `entitySetGravity(bool, targets)` | 重力开关 |
| `entitySetInvulnerable(bool)` / `entitySetInvulnerable(bool, targets)` | 无敌 |
| `entitySetGlowing(bool)` / `entitySetGlowing(bool, targets)` | 发光 |
| `entitySetSilent(bool)` / `entitySetSilent(bool, targets)` | 静音 |

---

## 药水效果

| 函数 | 说明 |
|------|------|
| `potion(type, level, duration)` / `potion(type, level, duration, targets)` | 施加药水效果 |
| `potionRemove(type)` / `potionRemove(type, targets)` | 移除药水效果 |

`type` = Bukkit PotionEffectType 枚举名，`level` = 等级（1-indexed），`duration` = ticks。

---

## 特效

| 函数 | 说明 |
|------|------|
| `freeze(ticks)` / `freeze(ticks, targets)` | 设置冻结 |
| `fire(ticks)` / `fire(ticks, targets)` | 设置着火 |
| `explosion(power)` / `explosion(power, fire, breakBlocks, targets)` | 创建爆炸 |

---

## 音效

| 函数 | 说明 |
|------|------|
| `sound(name)` / `sound(name, volume, pitch, targets)` | 播放 Bukkit Sound |
| `soundResource(name)` / `soundResource(name, volume, pitch, targets)` | 播放资源包音效 |

---

## 指令

| 函数 | 说明 |
|------|------|
| `command(cmd)` / `command(cmd, targets)` | 以目标身份执行 |
| `commandOp(cmd)` / `commandOp(cmd, targets)` | 以 OP 身份执行 |
| `commandConsole(cmd)` | 以控制台执行 |

---

## 弹射物

| 函数 | 说明 |
|------|------|
| `projectile(type)` / `projectile(type, speed, targets)` | 朝目标方向发射 |
| `projectileAt(type, x, y, z, speed)` | 朝固定向量发射 |
| `projectileToward(type, speed, sources, dests)` | 朝目标位置发射 |

支持 type: ARROW, FIREBALL, LARGE_FIREBALL, SMALL_FIREBALL, DRAGON_FIREBALL, WITHER_SKULL, SNOWBALL, EGG, ENDER_PEARL, TRIDENT, SPECTRAL_ARROW, SHULKER_BULLET, LLAMA_SPIT

---

## 速度

| 函数 | 说明 |
|------|------|
| `velocitySet(x, y, z)` / `velocitySet(x, y, z, targets)` | 设置速度向量 |
| `velocityAdd(x, y, z)` / `velocityAdd(x, y, z, targets)` | 叠加速度 |
| `velocityMove(x, y, z)` / `velocityMove(x, y, z, targets)` | 相对朝向移动 |
| `velocityZero()` / `velocityZero(targets)` | 速度归零 |
| `getVelocity(entity)` | 获取速度向量 |

---

## 状态效果

| 函数 | 说明 |
|------|------|
| `stateAttach(id, duration)` / `stateAttach(id, duration, refresh, targets)` | 附加状态 |
| `stateDetach(id)` / `stateDetach(id, layer, targets)` | 移除 N 层状态 |
| `stateRemove(id)` / `stateRemove(id, targets)` | 完全移除状态 |
| `stateHas(id)` / `stateHas(id, targets)` | 检查是否有某状态 |

---

## 目标查找器 (Finder)

```
var targets = finder()
    .range(5)          // 半径
    .includeSelf()     // 包含自身
    .type("ZOMBIE,SKELETON")  // 实体类型筛选
    .excludeType("player")    // 排除类型
    .name("pattern")   // 名称正则
    .inWorld("world")  // 世界筛选
    .sector(radius, angle, yaw)  // 扇形区域
    .limit(10)         // 数量限制
    .sort("DISTANCE")  // 排序（NAME/DISTANCE/RANDOM）
    .sortReverse()     // 反向排序
    .shuffle()         // 随机打乱
    .build()           // 返回 ProxyTargetContainer
```

---

## Metadata

| 函数 | 说明 |
|------|------|
| `hasMeta(key, entity)` | 检查存在 |
| `getMeta(key, entity)` | 获取值 |
| `setMeta(key, value, entity)` | 设置 |
| `setMetaTimeout(key, value, ticks, entity)` | 设置（自动过期） |
| `removeMeta(key, entity)` | 移除 |

---

## 经济 (Vault)

| 函数 | 说明 |
|------|------|
| `getBalance()` / `getBalance(player)` | 查询余额 |
| `takeMoney(amount)` / `takeMoney(amount, player)` | 扣款 |
| `giveMoney(amount)` / `giveMoney(amount, player)` | 加款 |
| `setMoney(amount)` / `setMoney(amount, player)` | 设置余额 |

---

## 属性 (AttributeDriver)

| 函数 | 说明 |
|------|------|
| `getAttr(name)` / `getAttr(name, targets)` | 读取属性值 |

---

## target 参数规则

大多数函数的 `targets` 是可选尾参。省略时按以下顺序查找：

1. `target` 变量存在 → 使用之
2. sender 是 entity → 使用 sender
3. 否则 → 使用 origin 位置
4. 部分函数 fallback 到所有在线玩家或控制台
