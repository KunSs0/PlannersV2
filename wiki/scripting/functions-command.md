# 命令函数

用于在脚本中执行 Minecraft 命令。

---

## command — 以目标身份执行

```javascript
command(cmd)                    // sender 执行命令
command(cmd, targets)           // 目标执行命令
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `cmd` | String | 命令内容（不需要加 `/`） |
| `targets` | ProxyTargetContainer | 可选。命令执行者 |

```javascript
// 让释放者执行命令
command("spawn")

// 让目标执行命令
command("say 我被控制了", targets)
```

## commandOp — 以 OP 权限执行

```javascript
commandOp(cmd)
commandOp(cmd, targets)
```

临时给予目标 OP 权限执行命令，执行完毕后恢复原权限。

```javascript
// 以 OP 权限执行（即使玩家没有权限）
commandOp("gamemode creative")
```

> **安全提示**：谨慎使用此函数，确保命令内容不会被玩家操控。

## commandConsole — 以控制台执行

```javascript
commandConsole(cmd)
```

以服务器控制台身份执行命令。

```javascript
// 以控制台身份给玩家物品
commandConsole("give " + sender.getName() + " diamond 1")

// 以控制台身份广播消息
commandConsole("say 有人释放了大招！")
```
