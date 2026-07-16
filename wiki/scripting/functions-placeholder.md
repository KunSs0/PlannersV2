# PlaceholderAPI 函数

需要安装并启用 PlaceholderAPI 插件。

---

## papi - 解析占位符

```javascript
papi(placeholder)
papi(placeholder, player)
```

返回 PlaceholderAPI 解析后的字符串。省略 `player` 时，使用当前技能的施法者。

```javascript
function main() {
  var playerName = papi("%player_name%")
  tell("&a当前玩家: " + playerName)
}
```

技能默认异步执行，`papi()` 会自动在 Bukkit 主线程完成解析后再返回结果。

当 PlaceholderAPI 未安装或未启用、施法者不是玩家且未传入第二个玩家参数时，脚本会明确报错。
