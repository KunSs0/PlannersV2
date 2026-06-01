# config.yml 完整配置参考

## database（数据库）

| 配置项 | 含义 | 默认值 |
|--------|------|--------|
| `database.use` | LOCAL(SQLite) / SQL(MySQL) | SQL |
| `database.sql.host` | MySQL 主机 | 127.0.0.1 |
| `database.sql.port` | MySQL 端口 | 3306 |
| `database.sql.user` | MySQL 用户名 | root |
| `database.sql.password` | MySQL 密码 | 123456 |
| `database.sql.database` | 数据库名 | bukkit_plugin |
| `database.sql.table` | 数据表前缀 | planners_v2 |

## settings.level（等级系统）

| 配置项 | 含义 |
|--------|------|
| `level.isolation` | `all` 全职业统一 / `router` 路线独立 / `job` 职业独立 |
| `level.synchronize` | 是否同步原版 Minecraft 等级 |
| `level.original-hook` | 是否兼容原版经验获取机制 |
| `level.algorithm` | isolation=all 时的经验算法 ID（引用 module/level/*.yml） |

## settings.skill-points（技能点系统）

| 配置项 | 含义 |
|--------|------|
| `per-level` | 每升一级获得技能点（JS 表达式，`level` 为变量） |
| `bonuses` | 关键等级额外奖励（Map：`等级 → 点数`） |

示例：`levl <= 30 ? 3 : 2`，10 级 +5，20 级 +5，30 级 +10，50 级 +15。

## settings.magic-point（法力值）

| 配置项 | 含义 |
|--------|------|
| `resume.expression` | 每周期恢复量（JS 表达式） |
| `resume.update-tick` | 恢复间隔（tick） |
| `upper-limit.expression` | 上限公式（JS，可用 `profile.level`） |
| `upper-limit.update-tick` | 上限更新间隔（tick） |

## settings.cooler（冷却）

| 配置项 | 含义 |
|--------|------|
| `cooler.use` | `memory`（重启清空）/ `persistence`（持久化） |

## settings.damage-causes（伤害类型）

```yaml
damage-causes: [SKILL, ATTRIBUTE, MYTHIC]
```

在此声明的自定义伤害类型才能被 `DamageCause.of()` 识别。JS 中可通过 `damageEx(amount, "SKILL")` 使用。

## settings.attribute.registry（属性注册表）

逻辑属性 → 物理属性的转换映射。共 13 个逻辑属性：

| 逻辑属性 | 映射物理属性 |
|----------|------------|
| STR（力量） | ATK=1.0, DEF=0.5 |
| INT（智力） | MAGIC_ATK=1.0, MANA_REGEN=0.3 |
| AGI（敏捷） | SPEED=0.8, CRIT=0.3 |
| END（耐力） | MAX_HEALTH=12.0, TOUGH=0.5, MAX_STAMINA=6.0 |
| CRIT（暴击） | CRIT=1.0 |
| STRIKE（强击） | STRIKE=1.0 |
| FAITH（信仰） | FAITH=1.0 |
| HASTE（急速） | HASTE=1.0 |
| TOUGH（坚韧） | TOUGH=1.0 |
| SPIRIT（精神） | SPIRIT=1.0 |
| HP（生命基数） | MAX_HEALTH=1.0 |
| MP（魔力基数） | MAX_MANA=1.0 |
| SP（体力基数） | MAX_STAMINA=1.0 |

## settings.placeholder（占位符）

| 配置项 | 含义 |
|--------|------|
| `placeholder.use` | `script`（JS 计算）/ `literal`（直接取值） |

## settings.attack.protect（攻击保护）

| 配置项 | 含义 |
|--------|------|
| `protect.enable` | 是否启用 |
| `protect.scene` | 生效场景（world / dungeonplus / team / worldguard 匹配） |

## settings.condition（通用条件池）

供技能树、转职引用的条件定义，每个条件包含：

| 字段 | 含义 |
|------|------|
| `exper` | JS 表达式，返回 true 时满足 |
| `props` | 参数（如 `min`、`skillId`、`amount`） |
| `hint` | 不满足时的提示文本 |
| `consume` | 满足后执行的消耗动作（可选） |

预定义条件：`player_lv`、`need_skill`、`consume_sp`、`need_coin`、`need_item`、`is_warrior`、`is_mage`。

## settings.keybinding（按键绑定）

### keymapping（按键注册表）

每个 key 节点：

| 字段 | 含义 |
|------|------|
| `name` | 显示名称 |
| `matching-type` | `strict`（顺序）/ `fuzzy`（无序）/ `none`（单键直通） |
| `request-tick` | 组合键输入窗口（tick） |
| `mapping` | Minecraft 按键代码 |

**实际 YAML 格式**（缩进式）：

```yaml
key0:
  name: R
  matching-type: strict
  request-tick: 30
  mapping: R
```

### backpack（技能背包）

| 字段 | 含义 |
|------|------|
| `default-page` | 默认页 ID |
| `pages.<id>.name` | 页名称 |
| `pages.<id>.slots.<slotN>.key` | 槽位 → keymapping 中的 key ID |

## settings.minecraft.interaction-action（交互动作）

| 配置项 | 含义 |
|--------|------|
| `enable` | 启用空手交互触发热键栏技能 |
| `empty-skill.material` | 空槽位材质 |
| `empty-skill.name` | 空槽位名称 |

## settings.bukkit-launch.unimpeded-types（穿透方块）

技能射线检测时穿透的方块类型。默认：`AIR, SHORT_GRASS, GLASS, WATER, DANDELION`。

---

## 配置目录结构

```
resources/
├── config.yml              # 核心配置（本文件）
├── lang/zh_CN.yml          # 语言文件（30 条消息）
├── skill/*.yml             # 技能配置
├── job/<路线>/*.yml         # 职业配置
├── router/*.yml            # 职业路由
├── skilltree/*.yml         # 技能树（节点+依赖图）
├── module/
│   ├── level/*.yml         # 等级经验算法
│   └── currency/*.yml      # 自定义货币
├── state/*.yml             # 状态效果
├── action/*.yml            # 自定义动作
└── ui/*.yml                # GUI 布局（7 个）
```

### UI 文件列表

| 文件 | 用途 |
|------|------|
| `ui/backpack.yml` | 技能背包 |
| `ui/backpack-skill-select.yml` | 背包技能选择 |
| `ui/route-transfer.yml` | 转职 |
| `ui/router-select.yml` | 职业选择 |
| `ui/skill-operator.yml` | 快捷键编辑 |
| `ui/skill-upgrade.yml` | 技能升级 |
| `ui/skilltree.yml` | 技能树 |

### lang/zh_CN.yml

覆盖职业、转职、经验、等级、法力、技能、UI、指令、校验等模块共 30 条消息，均支持 `&` 颜色码。
