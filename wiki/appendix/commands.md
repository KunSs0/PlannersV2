# 命令参考

主命令：`/planners`（别名：`/pl`、`/ps`）

权限节点：`planners.command`

---

## 技能命令

| 命令 | 说明 |
|------|------|
| `/planners skill open` | 打开技能面板 UI |
| `/planners skill upgrade <技能ID>` | 打开技能升级界面 |
| `/planners skill cast <技能ID>` | 释放已学习的技能 |
| `/planners skill run <玩家> <技能ID> [等级]` | 让指定玩家释放技能（管理员用） |

**示例**：
```
/planners skill open
/planners skill cast ground_slash
/planners skill run Steve ground_slash 5
```

---

## 职业命令

| 命令 | 说明 |
|------|------|
| `/planners route open` | 打开路由选择 UI |
| `/planners route select <路由ID>` | 直接选择路由 |
| `/planners route transfer` | 打开转职 UI |
| `/planners route clear` | 清空当前职业 |

**示例**：
```
/planners route open
/planners route select soldier
/planners route transfer
/planners route clear
```

---

## 档案命令

### 等级

| 命令 | 说明 |
|------|------|
| `/planners profile level add <玩家> <数量>` | 增加等级 |
| `/planners profile level take <玩家> <数量>` | 减少等级 |
| `/planners profile level set <玩家> <数量>` | 设置等级 |

### 经验

| 命令 | 说明 |
|------|------|
| `/planners profile experience add <玩家> <数量>` | 增加经验 |
| `/planners profile experience take <玩家> <数量>` | 减少经验 |
| `/planners profile experience set <玩家> <数量>` | 设置经验 |

### 法力值

| 命令 | 说明 |
|------|------|
| `/planners profile magicpoint add <玩家> <数量>` | 增加法力值 |
| `/planners profile magicpoint take <玩家> <数量>` | 减少法力值 |
| `/planners profile magicpoint set <玩家> <数量>` | 设置法力值 |
| `/planners profile magicpoint reset <玩家>` | 重置法力值 |

**示例**：
```
/planners profile level add Steve 5
/planners profile experience set Steve 10000
/planners profile magicpoint reset Steve
```

---

## 状态命令

| 命令 | 说明 |
|------|------|
| `/planners test <状态ID> <持续时间tick>` | 给自己附加状态（测试用） |
| `/planners state trigger <玩家> <触发器名>` | 触发自定义状态触发器 |

**示例**：
```
/planners test stun 100
/planners state trigger Steve my_trigger
```

---

## 元数据命令

| 命令 | 说明 |
|------|------|
| `/planners metadata <id>` | 查看元数据 |

---

## 其他命令

| 命令 | 说明 |
|------|------|
| `/planners reload` | 重载所有配置文件 |
| `/planners console` | 控制台调试命令 |
