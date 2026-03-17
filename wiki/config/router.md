# 路由与转职

路由文件位于 `plugins/Planners/router/` 目录，定义职业树的分支结构和转职条件。

---

## 什么是路由？

路由就是一条「职业发展路线」。比如：

```
战斗系（路由）
└── 剑士（初始职业）
    ├── 剑魂（转职分支 1）
    │   └── 剑圣（再次转职）
    └── 守护者（转职分支 2）
```

玩家选择一个路由后，会从初始职业开始，然后可以按照分支进行转职。

---

## 完整示例

以下是项目自带的 `router/soldier.yml`：

```yaml
# 路由全局配置
__option__:
  # 路由名称
  name: "战士"
  # 初始职业（对应下面的节点名 "swordsman"）
  originate: swordsman
  # 等级算法
  algorithm:
    level: def0
  # 路由图标（在路由选择 UI 中显示）
  icon:
    material: DIAMOND_SWORD
    name: "职业: 战士"
    lore:
      - '&f- &a基础数值'
      - "&f体力： &b|||||||"
      - "&f攻击： &b|||||"
      - "&f防御： &b|||||"
      - "&f机动： &b|||||"
      - '&f- &a转职路线'
      - "    &7> &f剑魂"
      - "    &7> &f剑圣"
      - ''
      - '&e难度： ||'

# ============ 职业节点 ============

# 初始职业：剑士
swordsman:
  # 可以转职到的下级职业
  branch:
    - blade-master
    - grand-master

# 转职职业：剑魂
blade-master:
  # 剑魂还可以继续转职到剑圣
  branch: grand-master
  # 转职条件
  condition:
    m0:
      if: "true"
      message: "金币所需100"
  # 职业图标
  icon:
    material: DIAMOND_SWORD
    name: "职业: 剑魂"
    lore:
      - '&f- &a基础数值'
      - "&f体力： &b|||||||"
      - "&f攻击： &b|||||"
      - "&f防御： &b|||||"
      - "&f机动： &b|||||"
      - '&f- &a转职路线'
      - "    &7> &f剑圣"
      - ''
      - '$message'
      - ''
      - '&e难度： ||'

# 最终职业：剑圣
grand-master:
  condition:
    m0:
      if: "false"
      message: "金币所需100"
  icon:
    material: DIAMOND_SWORD
    name: "职业: 剑圣"
    lore:
      - '&f- &a基础数值'
      - "&f体力： &b|||||||"
      - "&f攻击： &b|||||"
      - "&f防御： &b|||||"
      - "&f机动： &b|||||"
      - ''
      - '$message'
      - ''
      - '&e难度： ||'
```

---

## __option__ 字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 路由显示名称 |
| `originate` | String | 是 | 初始职业的节点名。玩家选择该路由后，默认进入这个职业 |
| `algorithm.level` | String | 否 | 等级算法 ID，对应 `module/level/` 中的定义 |
| `icon` | Map | 否 | 路由图标，在路由选择 UI 中显示 |

---

## 职业节点字段

每个职业节点以节点名作为 key（如 `swordsman`、`blade-master`）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `branch` | String 或 List | 否 | 可转职的下级职业节点名。单个用字符串，多个用列表 |
| `condition` | Map | 否 | 转职条件组（见下方说明） |
| `icon` | Map | 否 | 职业图标 |

> **重要**：节点名必须与 `job/` 目录下的职业文件名一致。例如节点名 `swordsman` 对应 `job/soldier/swordsman.yml`。

---

## 转职条件

条件组中每个条目包含 `if` 和 `message` 两个字段：

```yaml
condition:
  # 条件 ID（随意命名，不重复即可）
  level_check:
    # JS 表达式，返回 true 或 false
    if: "profile.level >= 30"
    # 条件不满足时显示给玩家的提示
    message: "&c等级不足，需要 30 级"
  money_check:
    if: "getBalance() >= 500"
    message: "&c金币不足，需要 500 金币"
```

- 多个条件之间是 **AND** 关系，必须全部满足才能转职
- `if` 中可以使用所有脚本全局函数和上下文变量
- `$message` 可以在图标 lore 中引用条件的 message 文本

---

## 图标配置

```yaml
icon:
  material: DIAMOND_SWORD     # 物品材质（Bukkit Material 枚举名）
  name: "&6职业名称"           # 物品名称（支持颜色代码）
  lore:                        # 物品描述（支持颜色代码）
    - "&7描述第一行"
    - "&7描述第二行"
    - '$message'               # 引用转职条件的 message
```

`$message` 是特殊占位符，会被替换为转职条件中的 `message` 文本。如果条件满足，该行不显示。

---

## 相关命令

| 命令 | 说明 |
|------|------|
| `/planners route open` | 打开路由选择 UI |
| `/planners route select <路由ID>` | 直接选择路由（路由 ID = 文件名） |
| `/planners route transfer` | 打开转职 UI |
| `/planners route clear` | 清空当前职业 |

---

## 常见问题

**Q：一个玩家可以同时拥有多个职业吗？**  
A：不可以。一个玩家同一时间只能拥有一个职业。

**Q：转职后等级会重置吗？**  
A：取决于 `config.yml` 中的 `level.isolation` 设置。如果是 `all`，等级不会重置；如果是 `job`，每个职业有独立等级。

**Q：`branch` 可以写多个吗？**  
A：可以。用列表格式：`branch: [blade-master, guardian]`，表示可以选择转职到剑魂或守护者。
