# 经济函数

需要安装 Vault 插件和对应的经济插件（如 EssentialsX）。

---

## getBalance — 获取余额

```javascript
getBalance()                    // 获取 sender 的余额
getBalance(player)              // 获取指定玩家的余额
```

返回值：Double，玩家的余额。

```javascript
var money = getBalance()
tell("你的余额: " + money)
```

## takeMoney — 扣除金币

```javascript
takeMoney(amount)               // 扣除 sender 的金币
takeMoney(amount, player)       // 扣除指定玩家的金币
```

```javascript
takeMoney(100)
tell("扣除 100 金币")
```

## giveMoney — 给予金币

```javascript
giveMoney(amount)               // 给 sender 金币
giveMoney(amount, player)       // 给指定玩家金币
```

```javascript
giveMoney(50)
tell("获得 50 金币")
```

## setMoney — 设置余额

```javascript
setMoney(amount)                // 设置 sender 的余额
setMoney(amount, player)       // 设置指定玩家的余额
```

---

## 完整示例

### 金币消耗技能

```javascript
function main() {
  var cost = 50 * level
  if (getBalance() < cost) {
    tell("&c金币不足！需要 " + cost + " 金币")
    return
  }

  takeMoney(cost)

  var targets = finder().range(10).excludeType("PLAYER").build()
  damage(100 + level * 50, targets)

  tell("&a消耗 " + cost + " 金币，释放强力一击！")
  setCooldown(skill, 200)
}
```
