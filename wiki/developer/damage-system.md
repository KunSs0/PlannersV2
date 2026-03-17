# 伤害系统

Planners 提供了自定义伤害系统，支持自定义伤害类型和伤害来源追踪。

---

## ProxyDamage

`ProxyDamage` 是伤害的核心类，使用 Builder 模式构建。

```kotlin
import com.gitee.planners.api.damage.ProxyDamage
import com.gitee.planners.api.damage.DamageCause

// 完整构建
ProxyDamage.builder()
    .source(attacker)           // 伤害来源（可选）
    .target(victim)             // 伤害目标
    .damage(50.0)               // 伤害值
    .cause(DamageCause.of("SKILL"))  // 伤害类型
    .build()
    .execute()                  // 执行伤害

// 快捷方法
ProxyDamage.of(target, 50.0)                           // 无来源，默认类型
ProxyDamage.of(source, target, 50.0)                   // 有来源
ProxyDamage.of(source, target, 50.0, cause)            // 完整参数
```

---

## DamageCause

自定义伤害类型。

### 创建伤害类型

```kotlin
import com.gitee.planners.api.damage.DamageCause

// 从 Bukkit 原生类型创建
val fire = DamageCause.of(EntityDamageEvent.DamageCause.FIRE)

// 从自定义名称创建（需要在 config.yml 中注册）
val skill = DamageCause.of("SKILL")
val attribute = DamageCause.of("ATTRIBUTE")
val mythic = DamageCause.of("MYTHIC")
```

### 注册自定义伤害类型

在 `config.yml` 中注册：

```yaml
settings:
  damage-causes:
    - SKILL
    - ATTRIBUTE
    - MYTHIC
    - MY_CUSTOM_CAUSE    # 你的自定义类型
```

也可以通过代码注册：

```kotlin
DamageCause.register("MY_CUSTOM_CAUSE")
```

### 在脚本中使用

```javascript
// 默认使用 SKILL 类型
damage(50, targets)

// 指定伤害类型
damageEx(50, "ATTRIBUTE", targets)

// 指定来源和类型
damageExBy(50, "MYTHIC", sender, targets)
```

---

## DamageResult

伤害执行后的结果。

```kotlin
val result: DamageResult = ProxyDamage.builder()
    .source(attacker)
    .target(victim)
    .damage(50.0)
    .cause(DamageCause.of("SKILL"))
    .build()
    .execute()
```

---

## 与事件系统的关系

当通过 `ProxyDamage` 造成伤害时，会触发 `PlayerDamageEntityEvent`（如果来源是玩家）。

```kotlin
@EventHandler
fun onDamage(e: PlayerDamageEntityEvent) {
    val player = e.player
    val entity = e.entity
    val damage = e.damage
    val cause = e.cause        // DamageCause 对象
}
```
