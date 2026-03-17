# 声音函数

用于播放声音效果。

---

## sound — 播放 Bukkit 声音

```javascript
sound(name)                                 // 播放给 sender
sound(name, volume)
sound(name, volume, pitch)
sound(name, volume, pitch, targets)         // 播放给目标
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | - | Bukkit Sound 枚举名称 |
| `volume` | Float | `1.0` | 音量（0.0 ~ 1.0） |
| `pitch` | Float | `1.0` | 音调（0.5 ~ 2.0，1.0 为正常） |
| `targets` | ProxyTargetContainer | sender | 声音播放位置/接收者 |

常用声音：

| 名称 | 说明 |
|------|------|
| `ENTITY_PLAYER_ATTACK_SWEEP` | 横扫攻击 |
| `ENTITY_PLAYER_LEVELUP` | 升级 |
| `ENTITY_GENERIC_EXPLODE` | 爆炸 |
| `ENTITY_PLAYER_HURT_FREEZE` | 冰冻伤害 |
| `ENTITY_EXPERIENCE_ORB_PICKUP` | 拾取经验 |
| `BLOCK_ANVIL_USE` | 铁砧使用 |
| `ENTITY_WITHER_SHOOT` | 凋零射击 |
| `ENTITY_ENDER_DRAGON_GROWL` | 末影龙咆哮 |

```javascript
// 播放横扫声音
sound("ENTITY_PLAYER_ATTACK_SWEEP", 1.0, 1.0)

// 低音调爆炸声
sound("ENTITY_GENERIC_EXPLODE", 1.0, 0.5)

// 在目标位置播放
sound("ENTITY_PLAYER_HURT_FREEZE", 1.0, 1.0, targets)
```

## soundResource — 播放资源包声音

```javascript
soundResource(name)
soundResource(name, volume)
soundResource(name, volume, pitch)
soundResource(name, volume, pitch, targets)
```

参数与 `sound` 相同，但 `name` 是资源包中的声音路径（如 `custom.skill.fire`）。

```javascript
// 播放资源包中的自定义声音
soundResource("custom.skill.fire", 1.0, 1.0)
```
