# `natural.*` 查询根调研（收集）

> 状态：调研已完成，该查询根已按[职业属性只是属性](方案设计-职业成长并入属性来源.md)的结论整体删除；本文保留当时的现状与使用面记录。
> 相关：`方案设计-物理属性直连.md`（`attr.*` 已改为直连 `AttributeDriver`）、`方案设计-能力槽收敛.md`（其余能力槽已收敛）。

---

## 一、它是什么

`natural.*` 是 Planners 动作查询引擎里的一个只读根（`QueryRoot.NATURAL`），语义是**职业自然成长**：

- 只包含当前职业 `hook.attributes` 在该玩家当前职业等级下的取值；
- 不包含技能 `hook.attributes`、技能树属性节点、装备、Buff；
- 返回 `Double?`，键是物理属性 ID（与 `attr.*` 同一套，例如 `natural.strength`、`natural.spirit`）；
- 与 `attr.*` 的差别就是"去掉了所有额外来源"，用于算**额外加成**：`attr.x - natural.x`。

`QueryRoot.NATURAL` 没有标记 `skillOnly`（`QueryRoot.kt:33`），因此技术上任何流程都能用，不限于技能。

---

## 二、当前链路

```
表达式 natural.<key>
 → QueryPath.queryNatural            QueryPath.kt:428-435
 → QueryScope.naturalReader(key)     QueryScope.kt:18,38
 → ActionScopeFactory 注入           ActionScopeFactory.kt:145-147
      { key -> ActionAttributeAccess.natural(player, key) }
 → ActionAttributeAccess.NaturalReader（install 槽）  ActionAttributeAccess.kt:17-58
 → FightCore: FightCoreQueryBridge.installNatural    FightCoreQueryBridge.kt:37-38
 → readNatural → readAttribute → AttributeDriver.get()（求和）  FightCoreQueryBridge.kt:88-90,64-79
```

要点：

- **只有一个宿主能安装**：`installNatural` 是单字段 `@Volatile`，后装覆盖先装；FightCore 停用时 `reset()` 回默认（返回 `null`）。
- 未安装读取器时 `natural.<key>` 返回 `null`，纯路径/条件按"缺失"处理，参与数值运算会报 `requires a number, got: null`。
- `QueryScope` 的各派生作用域（`withTarget`、`withPayload`、区域/定时器）都会复制 `naturalReader`。
- 测试只覆盖接线：`QueryExpressionTest` 用 mock reader 断言 `natural.STR = 10.0`（`QueryExpressionTest.kt:42-47,71-74`）。

---

## 三、当前实际行为（缺陷）

FightCore 的实现是：

```kotlin
private fun readNatural(player: Player, key: String): Double? {
    return readAttribute(player, key)   // = AttributeDriver.get(...) 求和
}
```

也就是**返回的是属性汇总值**，不是职业自然成长。后果：

| 后果 | 位置 | 说明 |
| --- | --- | --- |
| 主属性项失效 | `SkillDamagePlan.kt:75` | `mainAttribute.evaluate(currentMain - naturalMain, 0)`；差值恒 0，`linear-main-attribute` 在 0 处为 1.0，主属性加成整项消失 |
| 治疗量归零 | `knight_skill_12.yml:65` | `attr.spirit - natural.spirit` 恒 0，`fc.heal` 金额为 0 |
| 无法区分"职业为 0"与"查询失败" | 同上 | 两者都返回同一个汇总值 / null |

`fc.damage.skill` 与 `fc.damage.swept` 都强制要求 `main` 与 `natural` 同时声明（`FightCoreSkillDamageAction.kt:189-191`、`FightCoreSweptHitAction.kt:150-152`），因此这个缺陷影响所有声明了主属性的职业技能。

---

## 四、使用面清单

### 4.1 服务端配置（14 文件 / 16 处）

| 文件 | 行 | 键 | 用途 |
| --- | --- | --- | --- |
| `skill/knight_skill_1.yml` | 88 | `natural.strength` | `fc.damage.skill` 的 `natural` 参数 |
| `skill/knight_skill_2.yml` | 96 | `natural.strength` | 同上 |
| `skill/knight_skill_3.yml` | 111 / 113 / 115 | `natural.strength` | `fc.damage.swept` 三段 phase |
| `skill/knight_skill_4.yml` | 97 | `natural.strength` | `fc.damage.skill` |
| `skill/knight_skill_7.yml` | 141 | `natural.strength` | `fc.damage.skill` |
| `skill/knight_skill_12.yml` | 65 | `natural.spirit` | `fc.heal` 治疗公式 |
| `skill/ls_warrior_skill_1..8.yml` | 71-97 | `natural.strength` | `fc.damage.skill` |

### 4.2 规避写法（现存）

`skill/sage/sage_prayer_minor_bolt.yml:46-48` 直接用变量替代：

```yaml
# 当前宿主的natural读取器仍返回属性汇总值，不能作为职业自然成长；先用显式公式。
naturalIntelligence: "10 + (profile.level - 1) * 1"
```

这个公式与 `job/sage/sage.yml` 的 `intelligence` 成长公式重复，职业成长一改就要同步，属于临时绕行。

### 4.3 代码

- FightCore：`FightCoreSkillDamageAction`、`FightCoreSweptHitAction`→`SkillDamagePlan.classAmount`；`FightCoreQueryBridge.readNatural`。
- Planners：查询根与 scope 字段（见第二节），无其他消费方。
- 注意：`natural.*` 的键已随上一轮迁移为物理属性 ID（`natural.strength`、`natural.spirit`），代码不感知。

### 4.4 文档

| 文档 | 内容 |
| --- | --- |
| `.codex/skills/planners/SKILL.md:11` | 声称"职业自然主属性通过 `PlannersJob` 读取当前 Job 已编译属性表达式" |
| `.codex/skills/planners/SKILL.md:195` | 查询根表：`attr` / `natural` = 战斗属性 / 职业自然成长 |
| `references/fightcore-skill-formulas.md:12,50,62` | `main` / `natural` 参数说明与示例（示例仍写 `natural.STR`，未跟上物理键迁移） |
| `docs/plugins/FightCorePlugin/2026-11-13-planners-natural-attribute-query.md` | 本次缺陷的实服诊断报告（日期快照，未修改） |

**文档与代码不一致**：`PlannersJob` 在当前 Planners 源码里不存在（全仓库检索无命中），这条描述没有对应实现。

---

## 五、期望语义与可用数据源

### 5.1 期望

- 值 = 当前职业 `hook.attributes` 在当前**职业等级**下的求值结果；
- 只含职业，不含技能、节点、装备、Buff；
- 玩家无职业、键不存在、表达式求值失败时返回缺失（`null`），不返回伪造的 0；
- 与 `attr.*` 使用同一套物理属性键。

### 5.2 数据源（Planners 内部已经具备）

- `ImmutableJob.attributeExpressions: Map<String, QueryRef>`（`ImmutableJob.kt:79`），加载期从 `__option__.hook.attributes` 编译；
- 求值上下文与 `HookAttributeSource.applyJobAttributes` 完全一致：`ActionScopeFactory.forPlayer(player, skill=null, level=0, emptyMap(), null, "job-attribute")`；
- 职业等级由 `profile.level` 读取（`PlayerTemplate.level`，来自 `PlayerRouter.level`）；
- 因此 `natural.<key>` 可以实现为：取当前 Job → 取该 key 的 `QueryRef` → 用当前玩家上下文求值 → `QueryNumber` 校验。

需要注意：

- 求值必须在主线程（读档案、等级、查询根），与现有属性刷新同一约束；
- 转职、升级、重载后结果自然变化，无需额外失效；
- 与属性汇总的刷新时机无关，`natural.*` 是"按需求值"，不走 `AttributeProxy`。

---

## 六、可选方案

| 方案 | 做法 | 优点 | 代价 |
| --- | --- | --- | --- |
| **A（推荐）** | 删除 `installNatural` 与 FightCore 注入；`QueryScope.naturalReader` 内部改为读当前 Job 表达式 | 数据源彻底分离，`attr - natural` 有意义；不跨仓库；与 `attr.*` 直连驱动的方向一致 | 每次求值要跑一遍职业表达式（可在单个作用域内缓存） |
| B | 保留注入，改 FightCore 的 `readNatural` 读 Job 表达式 | 改动最小 | 需要 Planners 新增公开 API 暴露"职业自然值"，跨仓库，且仍是宿主注入形态 |
| C | 删除 `natural.*` 根，技能改用显式变量 | 概念最少 | 需要改 14 个技能的配置，失去通用能力，且公式重复回配置 |

A 的落地细节：

- `ActionScopeFactory` 里 `naturalReader = { key -> readJobNatural(template, key) }`；
- 新增内部方法：`template.playerRouter?.getCurrentJob()?.attributeExpressions?.get(key)` → `AttributeExpressions.evaluate(ref, scope)`；
- 求值上下文复用 `forPlayer(player, null, 0, ...)`，避免递归（job 表达式里不应出现 `natural.*`，加载期可不校验，运行期自引用会返回 null）；
- 同一 scope 内按 key 缓存，避免一条动作里多次求值。

---

## 七、验证与风险

修复会**实际改变数值**（主属性项从 1.0 变成有加成、治疗量从 0 变成正值），需要数值回归。

验收场景：

| 场景 | 预期 |
| --- | --- |
| 贤者 1 级、无装备/节点/Buff | `natural.intelligence = 10`；`attr.intelligence = 10` |
| 贤者 30 级 | `natural.intelligence = 39` |
| 30 级 + 装备智力 10 | `attr.intelligence = 49`，`natural.intelligence` 仍为 39 |
| 学习智力属性节点 / 获得智力 Buff | `attr` 增加，`natural` 不变 |
| 技能等级变化 | `natural` 不变 |
| 无职业 / 档案未加载 | 缺失（`null`），技能动作明确报错而不是按 0 计算 |
| 转职、升级、重载 | 重新求值，无旧值残留 |
| `knight_skill_12` 治疗 | 随 `attr.spirit - natural.spirit` 变化，不再恒 0 |
| `fc.damage.skill` | 主属性项按差值生效；`main - natural = 0` 时才为 1.0 |

风险：

- 职业 `hook.attributes` 里写了 `natural.*` 会形成自引用，需要明确返回缺失而不是递归；
- 同一动作多次引用 `natural.*` 会重复求值，需在作用域内缓存；
- 修复后伤害/治疗数值普遍上升，需要与策划确认预期区间。

---

## 八、待确认

1. 选 A / B / C（推荐 A）。
2. `natural.*` 是否限制为技能流程使用（当前 root 未限制）。
3. 无职业、表达式求值失败时的返回：缺失（推荐）还是 0。
4. 修复后是否同步把 `sage_prayer_minor_bolt` 的临时 `naturalIntelligence` 变量改回 `natural.intelligence`。
