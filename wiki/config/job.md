# 职业配置

职业文件位于 `plugins/Planners/job/` 目录，每个 `.yml` 文件定义一个职业。支持子目录组织（例如 `job/soldier/swordsman.yml`）。

---

## 基本结构

```yaml
__option__:
  name: 战士
  skill:
    - warrior_slash_strike
    - warrior_shield_bash
    - warrior_power_strike
    - warrior_whirlwind
    - warrior_berserker_rage
    - warrior_combat_mastery
    - warrior_armor_expertise
    - warrior_weapon_mastery
    - warrior_battle_instinct
    - warrior_endurance
```

> 这是从项目自带的 `job/2025.7.9/warrior.yml` 中提取的真实示例。

---

## 字段说明

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | String | 是 | 文件名 | 职业的显示名称，玩家在 UI 中看到的名字 |
| `skill` | List\<String\> | 是 | 空列表 | 该职业拥有的技能 ID 列表 |

### 关于 `skill` 列表

- 每个值是技能文件的文件名（不含 `.yml` 后缀）
- 如果技能文件在子目录中（如 `skill/warrior/slash.yml`），ID 就是 `slash`（只取文件名）
- 玩家选择该职业后，只能使用列表中的技能
- 转职后，新职业的技能列表会替换旧的

### 关于文件名

文件名就是职业的 ID。例如：
- `job/swordsman.yml` → 职业 ID 为 `swordsman`
- `job/soldier/blade-master.yml` → 职业 ID 为 `blade-master`

---

## 完整示例

### 简单职业

```yaml
# job/mage.yml
__option__:
  name: 法师
  skill:
    - mage_fireball
    - mage_ice_shard
    - mage_lightning_bolt
    - mage_mana_shield
    - mage_blizzard
    - mage_meteor
    - mage_spell_power
    - mage_mana_mastery
    - mage_elemental_affinity
    - mage_arcane_knowledge
```

### 多个职业文件组织

你可以用子目录来组织职业文件：

```
job/
├── soldier/
│   ├── swordsman.yml       # 剑士（初始职业）
│   ├── blade-master.yml    # 剑魂（转职后）
│   └── grand-master.yml    # 剑圣（再次转职后）
├── mage.yml                # 法师
├── archer.yml              # 弓箭手
└── assassin.yml            # 刺客
```

每个文件都是独立的职业，通过 [路由配置](router.md) 来定义它们之间的转职关系。

---

## 常见问题

**Q：一个技能可以属于多个职业吗？**  
A：可以。只要在多个职业的 `skill` 列表中都写上同一个技能 ID 即可。

**Q：职业文件名可以用中文吗？**  
A：不建议。文件名会作为职业 ID 在命令和代码中使用，建议使用英文和连字符（如 `blade-master`）。

**Q：修改职业配置后需要重启服务器吗？**  
A：不需要。使用 `/planners reload` 命令即可热重载所有配置。
