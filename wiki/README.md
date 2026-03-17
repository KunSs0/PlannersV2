# Planners 文档

Planners 是一个基于 TabooLib 的 Minecraft 职业与技能系统插件。  
脚本引擎使用标准 JavaScript（Java 8~14 使用 Nashorn，Java 17+ 使用 GraalJS）。

---

## 新手入门

- [安装配置](getting-started/installation.md) — 环境要求、安装步骤、数据库配置
- [快速入门](getting-started/quickstart.md) — 从零跑通「创建职业 → 配技能 → 绑按键 → 释放」

## 配置参考

- [主配置 (config.yml)](config/main-config.md) — 数据库、等级、法力值、冷却、攻击保护等
- [职业配置](config/job.md) — 定义职业及其技能列表
- [路由与转职](config/router.md) — 职业树、转职条件、分支结构
- [技能配置](config/skill.md) — 技能图标、变量、升级条件、脚本
- [状态系统](config/state.md) — buff/debuff、层数叠加、生命周期回调
- [按键绑定](config/key-binding.md) — 按键映射、组合键、匹配模式
- [等级算法](config/level.md) — 自定义升级经验公式
- [货币系统](config/currency.md) — 对接 Vault 等经济插件

## 脚本系统

- [脚本引擎概述](scripting/overview.md) — JS 引擎选择、执行流程、基本语法
- [上下文变量](scripting/context-variables.md) — sender、profile、level、ctx、skill
- [目标查找器](scripting/target-finder.md) — finder() 链式 API 完整参考
- [通用函数](scripting/functions-common.md) — tell
- [战斗函数](scripting/functions-combat.md) — damage、heal、health、velocity、potion、effect
- [实体函数](scripting/functions-entity.md) — spawn、remove、teleport、AI、重力等
- [命令函数](scripting/functions-command.md) — command、commandOp、commandConsole
- [冷却函数](scripting/functions-cooldown.md) — getCooldown、setCooldown、resetCooldown
- [状态函数](scripting/functions-state.md) — stateAttach、stateDetach、stateRemove、stateHas
- [声音函数](scripting/functions-sound.md) — sound、soundResource
- [投射物函数](scripting/functions-projectile.md) — projectile、projectileAt、projectileToward
- [元数据函数](scripting/functions-metadata.md) — getMeta、setMeta、hasMeta、removeMeta
- [经济函数](scripting/functions-economy.md) — getBalance、takeMoney、giveMoney
- [兼容插件函数](scripting/functions-compat.md) — DragonCore、GermPlugin、MythicMobs、AttributePlus

## 开发者指南

- [API 参考](developer/api.md) — PlannersAPI、PlayerTemplateAPI、Registries
- [事件系统](developer/events.md) — 全部自定义事件详解
- [伤害系统](developer/damage-system.md) — ProxyDamage、DamageCause

## 附录

- [命令参考](appendix/commands.md) — 全部命令、参数、权限
- [兼容插件列表](appendix/compatibility.md) — 支持的第三方插件及功能
