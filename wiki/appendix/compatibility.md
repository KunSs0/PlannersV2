# 兼容插件列表

Planners 通过模块化兼容层支持多个第三方插件。安装对应插件后自动启用，无需额外配置。

---

## 经济系统

### Vault

| 功能 | 说明 |
|------|------|
| 余额查询 | `getBalance()` |
| 扣除货币 | `takeMoney(amount)` |
| 给予货币 | `giveMoney(amount)` |
| 设置余额 | `setMoney(amount)` |

用于技能升级消耗、转职条件判断等。需要同时安装 Vault 和具体的经济插件（如 EssentialsX、CMI 等）。

---

## 怪物系统

### MythicMobs

支持版本：4.x 和 5.x

| 功能 | 说明 |
|------|------|
| 伤害识别 | 识别 MythicMobs 怪物的伤害事件 |
| 生成怪物 | `mythic().spawnMob(name, location)` |
| 怪物检测 | `mythic().isMythicMob(entity)` |
| 状态机制 | 在 MythicMobs 技能中使用 Planners 状态 |

MythicMobs 技能中可用的 Planners 机制：

```yaml
# MythicMobs 技能配置
Skills:
  - plannersStateAttach{state=stun;duration=60} @target
  - plannersStateDetach{state=stun} @target
  - plannersStateCustomTrigger{name=demo} @target
  - plannersDamage{amount=50;cause=MYTHIC} @target
```

---

## 属性系统

### AttributePlus

支持版本：3.x

| 功能 | 说明 |
|------|------|
| 属性挂钩 | 职业和技能的 `hook.attributes` 自动同步 |
| 属性攻击 | `apAttack("damage=50", targets)` |

---

## 区域保护

### WorldGuard

支持版本：6.x 和 7.x

| 功能 | 说明 |
|------|------|
| 攻击保护 | 在 WorldGuard 保护区域内阻止技能伤害 |

在 `config.yml` 中配置：
```yaml
settings:
  attack:
    protect:
      scene:
        - worldguard xxx    # xxx 为区域名，* 为所有区域
```

### DungeonPlus

| 功能 | 说明 |
|------|------|
| 副本保护 | 在副本场景中进行攻击保护检查 |

---

## 客户端模组

### DragonCore

| 功能 | 说明 |
|------|------|
| 按键绑定 | 使用客户端自定义按键触发技能 |
| 粒子效果 | `dcParticle(scheme)` |
| 动画播放 | `dcAnimation(name)` |
| 声音播放 | `dcSound(name)` |
| 占位符同步 | `dcSync(data)` |

### GermPlugin（萌芽引擎）

| 功能 | 说明 |
|------|------|
| 特效播放 | `germEffect(name)` |
| 动画控制 | `germAnimation(name)` |
| 声音播放 | `germSound(name)` |
| 视角锁定 | `germViewLock()` / `germViewUnlock()` |
| 移动锁定 | `germMoveLock()` / `germMoveUnlock()` |
| 物品冷却 | `germCooldown(slot, tick)` |

### ModelEngine

| 功能 | 说明 |
|------|------|
| 模型应用 | 为实体应用自定义模型 |

---

## 变量系统

### PlaceholderAPI

| 功能 | 说明 |
|------|------|
| 占位符注册 | 自动注册 Planners 相关占位符 |
| 双向支持 | 在 Planners 配置中使用其他插件的占位符 |

占位符模式在 `config.yml` 中配置：
```yaml
settings:
  placeholder:
    use: script    # script 或 literal
```
