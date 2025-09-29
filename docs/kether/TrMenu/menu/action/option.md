---
description: 单条动作支持的参数
title: 参数
sidebar_position: 2
---

# 参数

## 延时

> ``{delay=[TICKS]}``

```yaml
# 延时 1sec 发送消息
- 'tell: Delayed Message {delay=20}'
```

## 概率

> ``{chance=[rate]}``

```yaml
# 使用概率为0.8（80%）的钻石物品
- 'give-item: material:DIAMOND {chance=0.8}'
```

## 条件

> ``{condition=`<Expression>`}``

```yaml
# 发送VIP用户消息，当条件满足时
- 'tell: VIP User Message {condition=perm *user.vip}'
```

## 遍历

> ``{players=`<Expression>`}``

```yaml
# 向所有玩家广播消息
- 'tell: A Broadcast Message {players}'

# 向所有管理员广播消息
- 'tell: An admin broadcast message {players: perm *admin}'
```

* 循环为所有满足条件的在线玩家执行该动作



