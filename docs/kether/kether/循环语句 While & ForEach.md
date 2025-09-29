---
title: 循环语句 While & ForEach
sidebar_position: 6
---

# 循环语句

> 使用循环语句的时候请务必小心，千万别陷入死循环了~

```
while {action} then {action}
```

<details>
    <summary>「展开 / 收起详情」💠 While 语句</summary>
    <iframe src={"https://www.yuque.com/sacredcraft/kether/reference?view=doc_embed&inner=oBP3u"} width={'100%'} height={'500'}/>
</details>

`while` 循环主要用在不确定循环次数的情景之下。

它在每一次循环前都会判断你给定的条件语句是否满足，若满足条件，则执行条件体，若不满足，则退出循环。

它的实际应用场景感觉有点少啊，各位可以自行去摸索一下。

## ForEach 循环

```
for {token} in {action} then {action}
```

<details>
    <summary>「展开 / 收起详情」💠 ForEach 语句</summary>
    <iframe src={"https://www.yuque.com/sacredcraft/kether/reference?view=doc_embed&inner=AP7ct"} width={'100%'} height={'500'}/>
</details>

`foreach` 循环主要用于遍历集合，例如遍历数组，或者遍历所有在线玩家，又或者根据给定范围遍历

我们来仔细看看这个语句的参数 `for {token} in then {action}`

第二个参数 `{action}` 需要传入被遍历集合

第一个参数 `{token}` 是指代每一次循环中，遍历到的当前内容的变量名。

这意味着你可以在循环体中通过 `&{token}` 获取当前遍历到的内容

第三个参数 `{action}` 自然就是循环体了，集合有多少个元素，循环体就会执行多少次。



```ruby
/* 遍历数组 */
set array to array [ 1 1 4 5 1 4 ]
for it in &array then {
    tell &it
}

/* 遍历在线玩家的名字 */
for playerName in players then {
  tell &playerName
}

/* 给定范围遍历 */
for it in range 1 to 10 then {
    tell &it
}
```



<h1 id="bSBPf">Break 跳出循环</h1>

如果你在循环的过程中，想要跳出（中断）循环，那么可以使用 `break` 语句。`While` 和 `ForEach` 都适用。

```ruby
/* 当遍历到 5 时跳出循环 */
for it in array [ 1 1 4 5 1 4 ] then {
    if check &it == 5 then break
    tell &it
}
```

运行结果：`1 1 4`



当然了，`break` 除了可以用在循环里，还可以用在 `join` 和 `map` 语句里。
