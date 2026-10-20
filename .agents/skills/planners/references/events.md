# 事件系统参考

## 技能释放事件

`PlayerSkillCastEvent`（sealed 容器类）：

| 子事件 | 触发时机 | 可取消 |
|--------|---------|--------|
| `PlayerSkillCastEvent.Check` | 最早阶段（CD/MP 检查前） | 是 |
| `PlayerSkillCastEvent.Pre` | CD/MP 检查后，资源锁定前 | 是 |
| `PlayerSkillCastEvent.Post` | execute 成功后 | 否 |

---

## 技能相关事件

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `PlayerSkillCooldownEvent.Set` | 冷却设置时 | 是 |
| `PlayerSkillEvent.LevelChange` | 技能等级变化 | 是 |

---

## 玩家相关事件

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `PlayerLevelChangeEvent` | 玩家等级变化 | 是 |
| `PlayerExperienceEvent.Increment` | 经验增加 | 是 |
| `PlayerExperienceEvent.Decrement` | 经验减少 | 是 |
| `PlayerExperienceEvent.Set` | 经验直接设置 | 是 |
| `PlayerExperienceEvent.Updated` | 经验更新完成 | 否 |
| `PlayerMagicPointEvent.Increase` | 法力增加 | 是 |
| `PlayerMagicPointEvent.Decrease` | 法力减少 | 是 |
| `PlayerMagicPointEvent.Set` | 法力直接设置 | 是 |
| `PlayerProfileLoadedEvent` | 玩家数据加载完成 | 否 |
| `PlayerSetRouteEvent.Pre` | 职业路线设置前 | 是 |
| `PlayerSetRouteEvent.Post` | 职业路线设置后 | 否 |
| `PlayerDamageEntityEvent` | 玩家伤害实体 | 是 |

---

## 背包相关事件

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `BackpackEquipEvent.Equip` | 技能装备前 | 是 |
| `BackpackEquipEvent.Unequip` | 技能卸下前 | 是 |
| `BackpackPageSwitchEvent.Pre` | 翻页前 | 是 |
| `BackpackPageSwitchEvent.Post` | 翻页后 | 否 |

---

## 状态相关事件

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `EntityStateEvent.Attach.Pre` | 状态附加前 | 是 |
| `EntityStateEvent.Attach.Post` | 状态附加后 | 否 |
| `EntityStateEvent.Detach.Pre` | 状态移除前 | 是 |
| `EntityStateEvent.Detach.Post` | 状态移除后 | 否 |
| `EntityStateEvent.Mount.Pre` | 状态首次挂载前 | 是 |
| `EntityStateEvent.Mount.Post` | 状态首次挂载后 | 否 |
| `EntityStateEvent.Close.Pre` | 状态完全关闭前 | 是 |
| `EntityStateEvent.Close.Post` | 状态完全关闭后 | 否 |
| `EntityStateEvent.End` | 状态自然到期 | 否 |

---

## 事件在 cast() 流程中的顺序

```
Check → Cooler检查 → MP检查 → Pre → 资源锁定 → (Hook拦截?) → execute → Post
  │                  │        │       │                              │
  ↓                  ↓        ↓       ↓                              ↓
CANCEL_WITH_EVENT  COOLING  MAGICPOINT  CANCEL_WITH_EVENT           SUCCESS
```
