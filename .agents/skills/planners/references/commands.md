# 命令完整参考

**根命令**：`/planners`（别名 `/pl`、`/ps`），权限 `planners.command`（所有子命令继承）。

## 技能操作

| 命令 | 说明 | 补全 |
|------|------|------|
| `/pl skill open <player>` | 打开技能操作 UI | 在线玩家 |
| `/pl skill tree <player>` | 打开技能树 UI | 在线玩家 |
| `/pl skill upgrade <player> <id>` | 打开技能升级 UI | 技能 ID |
| `/pl skill cast <player> <id>` | 释放技能（完整 CD/MP 流程） | 玩家已学技能 |
| `/pl skill run <player> <id> [level]` | 执行脚本（绕过检查，默认 1 级） | 所有注册技能 |

## 背包

| 命令 | 说明 | 补全 |
|------|------|------|
| `/pl backpack open <player>` | 打开背包 UI | 在线玩家 |
| `/pl backpack page <player> <page>` | 切换背包页并刷新物品栏 | 背包页 ID |

## 职业与转职

| 命令 | 说明 | 补全 |
|------|------|------|
| `/pl route open <player>` | 打开职业选择 UI | 在线玩家 |
| `/pl route select <player> <router>` | 直接设置职业路线 | 路由 ID |
| `/pl route transfer <player>` | 打开转职 UI | 在线玩家 |
| `/pl route clear <player>` | 清除职业路线 | 在线玩家 |

## 玩家属性管理（profile）

| 命令 | 说明 |
|------|------|
| `/pl profile level add <player> <value>` | 增加等级 |
| `/pl profile level take <player> <value>` | 减少等级 |
| `/pl profile level set <player> <value>` | 设置等级 |
| `/pl profile experience add <player> <value>` | 增加经验 |
| `/pl profile experience take <player> <value>` | 减少经验 |
| `/pl profile experience set <player> <value>` | 设置经验 |
| `/pl profile magicpoint add <player> <value>` | 增加法力 |
| `/pl profile magicpoint take <player> <value>` | 减少法力 |
| `/pl profile magicpoint set <player> <value>` | 设置法力 |
| `/pl profile magicpoint reset <player>` | 重置法力到上限 |

`/pl profile mp ...` 是 `/pl profile magicpoint ...` 的别名。

## 状态与测试

| 命令 | 说明 |
|------|------|
| `/pl state trigger <player> <name>` | 触发指定状态 |
| `/pl test <state> <duration>` | 为自己附加状态（Player only） |

## 其他

| 命令 | 说明 |
|------|------|
| `/pl main` | 显示命令帮助 |
| `/pl reload` | 重载全部配置 |
| `/pl console cast <id> [level]` | 以控制台身份执行技能 |
