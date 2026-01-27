# Fluxon 扩展函数待实现列表

基于现有配置文件分析，以下扩展函数需要补充实现才能完整迁移所有脚本。

## 已实现 ✅

### Entity 扩展 (`EntityExtensions.kt`)
- `id()` - 获取实体 ID
- `name()` - 获取实体名称
- `location()` - 获取实体位置
- `world()` - 获取实体世界
- `uuid()` - 获取实体 UUID
- `health()` - 获取生命值
- `maxHealth()` - 获取最大生命值
- `isDead()` - 是否死亡
- `setHealth(double)` - 设置生命值
- `damage(double)` - 造成伤害
- `heal(double)` - 治疗

### Location 扩展 (`LocationExtensions.kt`)
- `x()`, `y()`, `z()` - 获取坐标
- `yaw()`, `pitch()` - 获取朝向
- `world()` - 获取世界
- `block()` - 获取方块
- `clone()` - 克隆位置
- `add(double, double, double)` - 偏移
- `distance(Location)` - 计算距离

### Common 扩展 (`CommonExtensions.kt`)
- `Player::playSound(string)` - 播放声音
- `Player::playSound(string, float, float)` - 播放声音（带音量和音调）
- `Location::playSound(string, float, float)` - 在位置播放声音

### Sender 扩展 (`SenderExtensions.kt`)
- `sendMessage(string)` - 发送消息
- `hasPermission(string)` - 检查权限
- `name()` - 获取名称

### Player 扩展 (`PlayerExtensions.kt`) ✅
- `yaw()` - 获取玩家朝向 yaw
- `pitch()` - 获取玩家朝向 pitch
- `lookLocation(distance: Double)` - 获取玩家视线位置
- `name()` - 获取玩家名称
- `uuid()` - 获取玩家 UUID
- `eyeLocation()` - 获取玩家眼睛位置
- `teleport(Location)` - 传送玩家

### 元数据操作 (`MetadataExtensions.kt`) ✅
- `Entity::hasMetadata(key: String) : Boolean`
- `Entity::getMetadata(key: String) : Any?`
- `Entity::setMetadata(key: String, value: Any)`
- `Entity::setMetadata(key: String, value: Any, timeout: Long)`
- `Entity::removeMetadata(key: String)`
- `Entity::metadataContains(key: String, value: String) : Boolean`

### Profile 操作 (`ProfileExtensions.kt`) ✅
- `Player::getMagicPoint() : Int` - 获取法力值
- `Player::setMagicPoint(value: Int)` - 设置法力值
- `Player::takeMagicPoint(amount: Int)` - 消耗法力值
- `Player::giveMagicPoint(amount: Int)` - 恢复法力值
- `Player::getMaxMagicPoint() : Int` - 获取最大法力值
- `Player::setMaxMagicPoint(value: Int)` - 设置最大法力值

### 冷却系统 (`CooldownExtensions.kt`) ✅
- `Player::getCooldown(skill: Skill/String) : Long`
- `Player::setCooldown(skill: Skill/String, ticks: Int)`
- `Player::resetCooldown(skill: Skill/String)`
- `Player::hasCooldown(skill: Skill/String) : Boolean`

### 命令执行 (`CommandExtensions.kt`) ✅
- `Player::executeCommand(command: String)` - 以玩家身份执行命令
- `Player::executeCommandAsOp(command: String)` - 临时 OP 执行命令
- `String::executeAsConsole()` - 以控制台身份执行命令

### 延迟等待 (`DelayExtensions.kt`) ✅
- `"wait"::delay(duration: String)` - 延迟等待（支持 "0.4s", "10t", "200ms"）
- `"wait"::delayTicks(ticks: Long)` - 按 tick 延迟
- `"wait"::delayMillis(millis: Long)` - 按毫秒延迟

### 数学函数 (`MathExtensions.kt`) ✅
- `abs(value: Number) : Double` - 绝对值
- `max(a: Number, b: Number) : Double` - 最大值
- `min(a: Number, b: Number) : Double` - 最小值
- `sqrt(value: Number) : Double` - 平方根
- `sin(value: Number) : Double` - 正弦
- `cos(value: Number) : Double` - 余弦
- `tan(value: Number) : Double` - 正切

### 速度控制 (`VelocityExtensions.kt`) ✅
- `Entity::setVelocity(x: Double, y: Double, z: Double)` - 设置速度
- `Entity::addVelocity(x: Double, y: Double, z: Double)` - 添加速度
- `Entity::getVelocity() : Vector` - 获取速度

### 选择器系统 (`SelectorExtensions.kt`) ✅
- `selectRectangle(width, height, length, location, filter)` - 矩形选择器
- `selectSphere(radius, location, filter)` - 球形选择器
- `selectLine(distance, location, direction, filter)` - 直线选择器

### 技能系统 (`SkillSystemExtensions.kt`) ✅
- `apAttack(params: String, targets: List<Entity>)` - 属性攻击
- `getSkillLevel(player: Player, skill: Skill) : Int` - 获取技能等级
- `setSkillLevel(player: Player, skill: Skill, level: Int)` - 设置技能等级

### MythicMobs 集成 (`MythicMobsExtensions.kt`) ✅
- `spawnMythicMob(mobType: String, location: Location)` - 生成 MythicMob
- `sendMythicSignal(signal: String, location: Location)` - 发送信号
- `isMythicMob(entity: Entity) : Boolean` - 检查是否为 MythicMob

### GermPlugin 集成 (`GermPluginExtensions.kt`) ✅
- `Player::playGermModel(model: String)` - 播放模型动画
- `Player::stopGermModel(model: String)` - 停止模型动画
- `Player::playGermEffect(effect: String)` - 播放特效
- `Player::stopGermEffect(effect: String)` - 停止特效
- `Player::playGermSound(sound: String)` - 播放音效

### 经济系统 (`EconomyExtensions.kt`) ✅
- `Player::getBalance() : Double` - 获取余额
- `Player::takeBalance(amount: Double) : Boolean` - 扣除金额
- `Player::giveBalance(amount: Double) : Boolean` - 增加金额
- `Player::setBalance(amount: Double)` - 设置金额

### 数学函数 (`MathExtensions.kt`) ✅
- `Double::abs() : Double` - 绝对值
- `Double::max(b: Double) : Double` - 最大值
- `Double::min(b: Double) : Double` - 最小值
- `Double::sqrt() : Double` - 平方根
- `Double::sin() : Double` - 正弦
- `Double::cos() : Double` - 余弦
- `Double::tan() : Double` - 正切
- `Math::random(min: Int, max: Int) : Int` - 随机整数

## 实现优先级说明

### 高优先级 ✅ 已完成
这些功能在配置文件中大量使用，必须先实现才能进行脚本迁移：
- ✅ Player 扩展
- ✅ 元数据操作
- ✅ Profile 操作
- ✅ 冷却系统
- ✅ 命令执行
- ✅ 等待和延迟

### 中优先级 ✅ 已完成
这些功能在部分技能中使用，影响中等：
- ✅ 选择器系统
- ✅ 速度控制
- ✅ 技能系统
- ✅ 数学函数

### 低优先级 ✅ 已完成
这些是可选的第三方集成，可以逐步实现：
- ✅ MythicMobs 集成
- ✅ GermPlugin 集成

## 实现建议

1. **分模块实现**：每个功能类别独立一个文件
2. **参考原 Kether 实现**：可查看 `git show HEAD~1:src/main/kotlin/com/gitee/planners/module/kether/`
3. **测试驱动**：实现一个扩展后，立即迁移使用它的配置文件进行测试
4. **文档同步**：每实现一个扩展，更新此文档的完成状态

## 迁移策略

### 第一阶段：基础扩展
实现高优先级扩展，迁移简单配置文件（如 level、currency、action 示例）

### 第二阶段：技能系统
实现中优先级扩展，迁移技能配置文件

### 第三阶段：完整集成
实现低优先级扩展，迁移所有剩余配置文件

## 当前状态

- ✅ 基础扩展已实现 (Entity, Location, Common, Sender)
- ✅ 高优先级扩展已完成 (Player, Metadata, Profile, Cooldown, Command, Delay)
- ✅ 中优先级扩展已完成 (Math, Velocity, Selector, SkillSystem, Economy)
- ✅ 低优先级扩展已完成 (MythicMobs, GermPlugin)

**总进度**: 17/17 (100%) 🎉

## 配置文件迁移状态

### 已完成迁移 ✅
- ✅ 法师技能 (10个): mage_fireball, mage_ice_shard, mage_blizzard, mage_lightning_bolt, mage_meteor, mage_arcane_knowledge, mage_elemental_affinity, mage_mana_mastery, mage_mana_shield, mage_spell_power
- ✅ 战士技能 (10个): warrior_power_strike, warrior_slash_strike, warrior_shield_bash, warrior_whirlwind, warrior_berserker_rage, warrior_armor_expertise, warrior_battle_instinct, warrior_combat_mastery, warrior_endurance, warrior_weapon_mastery
- ✅ 刺客技能 (1个): assassin_backstab
- ✅ 职业配置: archer, assassin, guardian, mage, warrior, blade-master, grand-master, swordsman
- ✅ 示例配置: skill/example0.yml, skill/example1.yml, action/example0.yml, module/level/example.yml, module/currency/example.yml
- ✅ 核心配置: config.yml (magic-point 配置)

**配置迁移进度**: 所有配置文件已完成迁移 ✅
