# 兼容插件函数

以下函数需要安装对应的第三方插件才能使用。如果未安装对应插件，调用这些函数会报错。

---

## DragonCore 函数

需要安装 DragonCore 客户端模组和服务端插件。

### dcParticle — 播放粒子

```javascript
dcParticle(scheme)
dcParticle(scheme, x, y, z, tile, targets)
```

### dcSound — 播放声音

```javascript
dcSound(name)
dcSound(name, id, type, volume, pitch, loop, targets)
```

### dcAnimation — 播放动画

```javascript
dcAnimation(name)
dcAnimation(name, transition, targets)
```

### dcAnimationRemove — 移除动画

```javascript
dcAnimationRemove(name)
dcAnimationRemove(name, transition, targets)
```

### dcPlayerAnimation — 播放玩家动画

```javascript
dcPlayerAnimation(name)
dcPlayerAnimation(name, targets)
```

### dcPlayerAnimationRemove — 移除玩家动画

```javascript
dcPlayerAnimationRemove()
dcPlayerAnimationRemove(targets)
```

### dcSync — 同步占位符

```javascript
dcSync(data)
dcSync(data, targets)
```

### dcSyncDelete — 删除占位符缓存

```javascript
dcSyncDelete(name)
dcSyncDelete(name, isStartWith, targets)
```

### dcEntityFunction — 执行实体函数

```javascript
dcEntityFunction(func)
dcEntityFunction(func, targets)
```

---

## GermPlugin 函数

需要安装 GermPlugin（萌芽引擎）。

### germEffect — 播放效果

```javascript
germEffect(name)
germEffect(name, index, targets)
```

### germEffectRemove — 移除效果

```javascript
germEffectRemove(index)
germEffectRemove(index, targets)
```

### germEffectClear — 清空所有效果

```javascript
germEffectClear()
germEffectClear(targets)
```

### germSound — 播放声音

```javascript
germSound(name)
germSound(name, type, volume, pitch, targets)
```

### germAnimation — 播放动画

```javascript
germAnimation(name)
germAnimation(name, speed, reverse, targets)
```

### germAnimationStop — 停止动画

```javascript
germAnimationStop(name)
germAnimationStop(name, targets)
```

### germViewLock / germViewUnlock — 视角锁定

```javascript
germViewLock()
germViewLock(duration, type, targets)
germViewUnlock()
germViewUnlock(targets)
```

### germLookLock / germLookUnlock — 视线锁定

```javascript
germLookLock()
germLookLock(duration, targets)
germLookUnlock()
germLookUnlock(targets)
```

### germMoveLock / germMoveUnlock — 移动锁定

```javascript
germMoveLock()
germMoveLock(duration, targets)
germMoveUnlock()
germMoveUnlock(targets)
```

### germCooldown — 物品冷却

```javascript
germCooldown(slot, tick)
germCooldown(slot, tick, targets)
```

---

## MythicMobs 函数

需要安装 MythicMobs（支持 4.x 和 5.x）。

### mythic — 获取 MythicMobs 对象

```javascript
var mm = mythic()
```

返回 MythicObject 单例，提供以下方法：

```javascript
// 在指定位置生成神话生物
mm.spawnMob("SkeletonKing", location)

// 检查实体是否为神话生物
var isMythic = mm.isMythicMob(entity)
```

---

## AttributePlus 函数

需要安装 AttributePlus 3.x。

### apAttack — 属性攻击

```javascript
apAttack(params, targets)
apAttack(params, targets, source)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `params` | String | 攻击参数，格式为 `key=value,key=value` |
| `targets` | Object | 攻击目标 |
| `source` | Object | 可选。攻击来源 |

```javascript
// 对目标造成 50 点属性伤害
var targets = finder().range(5).excludeType("PLAYER").build()
apAttack("damage=50", targets)

// 指定来源
apAttack("damage=100", targets, sender)
```
