# 事件系统

Planners 基于 Bukkit 事件系统提供了丰富的自定义事件，位于 `com.gitee.planners.api.event` 包下。

所有带 `Pre` 后缀的事件都可以通过 `isCancelled = true` 取消。

---

## 技能事件

### PlayerSkillCastEvent — 技能释放

```kotlin
// 技能释放前（可取消）
@EventHandler
fun onSkillCastPre(e: PlayerSkillCastEvent.Pre) {
    val player = e.player       // 释放者
    val skill = e.skill         // 技能对象
    e.isCancelled = true        // 取消释放
}

// 技能释放后
@EventHandler
fun onSkillCastPost(e: PlayerSkillCastEvent.Post) {
    val player = e.player
    val skill = e.skill
    // 技能已成功释放，可以做日志记录等
}
```

### PlayerSkillCooldownEvent — 技能冷却

```kotlin
@EventHandler
fun onCooldownSet(e: PlayerSkillCooldownEvent.Set) {
    val player = e.player
    val skill = e.skill
    val ticks = e.ticks         // 冷却时间（tick）
}
```

### PlayerSkillEvent — 技能等级/绑定变化

```kotlin
// 技能等级变化
@EventHandler
fun onSkillLevelChange(e: PlayerSkillEvent.LevelChange) {
    val player = e.player
    val skill = e.skill
    val from = e.form           // 原等级
    val to = e.to               // 新等级
    player.sendMessage("技能等级: $from -> $to")
}

// 技能绑定变化
@EventHandler
fun onSkillBindingChange(e: PlayerSkillEvent.BindingChange) {
    val player = e.player
    val skill = e.skill
    val binding = e.binding     // 新绑定的按键（null = 解绑）
}
```

---

## 职业事件

### PlayerSetRouteEvent — 职业选择

```kotlin
// 选择职业前（可取消）
@EventHandler
fun onSetRoutePre(e: PlayerSetRouteEvent.Pre) {
    val player = e.player
    val route = e.route
    // 阻止选择某个职业
    if (route.id == "forbidden") {
        e.isCancelled = true
    }
}

// 选择职业后
@EventHandler
fun onSetRoutePost(e: PlayerSetRouteEvent.Post) {
    val player = e.player
    val route = e.route
    player.sendMessage("你选择了 ${route.id}")
}
```

---

## 等级与经验事件

### PlayerLevelChangeEvent — 等级变化

```kotlin
@EventHandler
fun onLevelChange(e: PlayerLevelChangeEvent) {
    val player = e.player
    val from = e.form           // 原等级
    val to = e.to               // 新等级
    if (to > from) {
        player.sendMessage("恭喜升级！$from -> $to")
    }
}
```

### PlayerExperienceEvent — 经验变化

有四个子类型：

```kotlin
// 经验增加
@EventHandler
fun onExpInc(e: PlayerExperienceEvent.Increment) {
    val player = e.player
    val amount = e.amount       // 增加的经验量
}

// 经验减少
@EventHandler
fun onExpDec(e: PlayerExperienceEvent.Decrement) {
    val amount = e.amount
}

// 经验设置
@EventHandler
fun onExpSet(e: PlayerExperienceEvent.Set) {
    val value = e.value         // 设置后的经验值
}

// 经验更新（任何经验变化后都会触发）
@EventHandler
fun onExpUpdated(e: PlayerExperienceEvent.Updated) {
    val player = e.player
    // 经验已更新，可以刷新 UI 等
}
```

---

## 法力值事件

### PlayerMagicPointEvent — 法力值变化

```kotlin
// 法力值增加
@EventHandler
fun onMpIncrease(e: PlayerMagicPointEvent.Increase) {
    val player = e.player
    val amount = e.amount
}

// 法力值减少
@EventHandler
fun onMpDecrease(e: PlayerMagicPointEvent.Decrease) {
    val amount = e.amount
}

// 法力值设置
@EventHandler
fun onMpSet(e: PlayerMagicPointEvent.Set) {
    val value = e.value
}
```

---

## 状态事件

### EntityStateEvent — 实体状态变化

每个阶段都有 Pre（可取消）和 Post 两个时机：

```kotlin
// 状态首次附加（0 层 → 1 层）
@EventHandler
fun onStateMount(e: EntityStateEvent.Mount.Pre) {
    val entity = e.entity       // ProxyTarget.BukkitEntity
    val state = e.state         // State 对象
    // e.isCancelled = true     // 取消附加
}

@EventHandler
fun onStateMountPost(e: EntityStateEvent.Mount.Post) {
    // 状态已成功首次附加
}

// 状态层数增加
@EventHandler
fun onStateAttach(e: EntityStateEvent.Attach.Pre) {
    val entity = e.entity
    val state = e.state
}

@EventHandler
fun onStateAttachPost(e: EntityStateEvent.Attach.Post) {
    // 层数已增加
}

// 状态层数减少
@EventHandler
fun onStateDetach(e: EntityStateEvent.Detach.Pre) {
    val entity = e.entity
    val state = e.state
}

@EventHandler
fun onStateDetachPost(e: EntityStateEvent.Detach.Post) {
    // 层数已减少
}

// 状态完全移除（层数归零）
@EventHandler
fun onStateClose(e: EntityStateEvent.Close.Pre) {
    val entity = e.entity
    val state = e.state
}

@EventHandler
fun onStateClosePost(e: EntityStateEvent.Close.Post) {
    // 状态已完全移除
}

// 状态自然到期（不可取消）
@EventHandler
fun onStateEnd(e: EntityStateEvent.End) {
    val entity = e.entity
    val state = e.state
    // 到期后会自动调用 remove，触发 Detach + Close
}
```

---

## 伤害事件

### PlayerDamageEntityEvent — 玩家伤害实体

```kotlin
@EventHandler
fun onDamage(e: PlayerDamageEntityEvent) {
    val player = e.player       // 攻击者
    val entity = e.entity       // 被攻击的实体
    val damage = e.damage       // 伤害值
    val cause = e.cause         // 伤害类型（DamageCause）
}
```

### TargetCapturedEvent — 目标被捕获

当选择器选取到目标时触发。

```kotlin
@EventHandler
fun onCaptured(e: TargetCapturedEvent) {
    val container = e.container // 目标容器
    val cause = e.cause         // 捕获原因
}
```

---

## 系统事件

### PlayerProfileLoadedEvent — 玩家档案加载

玩家进入服务器、档案从数据库加载完成后触发。

```kotlin
@EventHandler
fun onProfileLoaded(e: PlayerProfileLoadedEvent) {
    val player = e.player
    val template = e.template   // PlayerTemplate
    // 可以在这里初始化玩家数据
}
```

### PluginReloadEvents — 插件重载

```kotlin
@EventHandler
fun onReloadPre(e: PluginReloadEvents.Pre) {
    // 重载前，清理缓存等
}

@EventHandler
fun onReloadPost(e: PluginReloadEvents.Post) {
    // 重载后，重新初始化
}
```

### DatabaseInitEvent — 数据库初始化

```kotlin
@EventHandler
fun onDbInit(e: DatabaseInitEvent) {
    // 数据库已就绪，可以执行数据库操作
}
```

### EntityModelApplyEvent — 模型应用

```kotlin
@EventHandler
fun onModelApply(e: EntityModelApplyEvent) {
    // 实体模型被应用时触发（需要 ModelEngine）
}
```

### CombinedEvent — 组合键事件

```kotlin
@EventHandler
fun onCombined(e: CombinedEvent) {
    // 玩家完成组合键输入时触发
}
```

### ProxyClientKeyEvents — 客户端按键事件

```kotlin
@EventHandler
fun onClientKey(e: ProxyClientKeyEvents) {
    // 客户端按键事件（DragonCore 等）
}
```

### ScriptCustomTriggerEvent — 自定义脚本触发

```kotlin
@EventHandler
fun onCustomTrigger(e: ScriptCustomTriggerEvent) {
    // 通过命令或脚本触发的自定义事件
}
```
