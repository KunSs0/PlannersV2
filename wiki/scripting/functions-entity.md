# 实体函数

用于生成、移除、传送实体以及控制实体属性。

---

## entitySpawn — 生成实体

```javascript
entitySpawn(type)                           // 在 sender 位置生成，永久存在
entitySpawn(type, duration)                 // 在 sender 位置生成，指定存活时间
entitySpawn(type, duration, locations)      // 在指定位置生成
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | String | 实体类型（Bukkit EntityType 名称，如 `ZOMBIE`、`SKELETON`） |
| `duration` | Int | 存活时间（tick）。`-1` 或不传表示永久 |
| `locations` | ProxyTargetContainer | 可选。生成位置 |

```javascript
// 在自己位置生成一个僵尸
entitySpawn("ZOMBIE")

// 生成一个 10 秒后消失的骷髅
entitySpawn("SKELETON", 200)

// 在目标位置生成苦力怕
var locs = finder().range(10).type("PLAYER").limit(1).build()
entitySpawn("CREEPER", 100, locs)
```

## entityRemove — 移除实体

```javascript
entityRemove()                              // 移除 sender
entityRemove(targets)                       // 移除目标实体
```

```javascript
var targets = finder().range(5).type("ZOMBIE").build()
entityRemove(targets)
```

## entityTeleport — 传送实体

```javascript
entityTeleport(x, y, z)                     // 传送 sender 到坐标
entityTeleport(x, y, z, targets)            // 传送目标到坐标
```

```javascript
// 传送自己到坐标 (100, 65, 200)
entityTeleport(100, 65, 200)
```

## entityTeleportTo — 传送到目标位置

```javascript
entityTeleportTo(destinations)              // 传送 sender 到目标位置
entityTeleportTo(targets, destinations)     // 传送 targets 到 destinations
```

```javascript
// 传送自己到最近的僵尸位置
var zombie = finder().range(20).type("ZOMBIE").sort("DISTANCE").limit(1).build()
entityTeleportTo(zombie)
```

## entitySetAI — 启用/禁用 AI

```javascript
entitySetAI(enabled)
entitySetAI(enabled, targets)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `enabled` | Boolean | `true` = 启用 AI，`false` = 禁用 AI |

```javascript
// 禁用目标的 AI（使其无法移动和攻击）
entitySetAI(false, targets)

// 3 秒后恢复
sleep(3000)
entitySetAI(true, targets)
```

## entitySetGravity — 启用/禁用重力

```javascript
entitySetGravity(enabled)
entitySetGravity(enabled, targets)
```

```javascript
// 让目标漂浮（禁用重力）
entitySetGravity(false, targets)
```

## entitySetInvulnerable — 启用/禁用无敌

```javascript
entitySetInvulnerable(enabled)
entitySetInvulnerable(enabled, targets)
```

## entitySetGlowing — 启用/禁用发光

```javascript
entitySetGlowing(enabled)
entitySetGlowing(enabled, targets)
```

```javascript
// 让目标发光 5 秒
entitySetGlowing(true, targets)
sleep(5000)
entitySetGlowing(false, targets)
```

## entitySetSilent — 启用/禁用静音

```javascript
entitySetSilent(enabled)
entitySetSilent(enabled, targets)
```

使实体不发出声音。
