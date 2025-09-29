---
title: 内联脚本语句 Inline
sidebar_position: 6
---

# 内联脚本语句

<details>
    <summary>「展开 / 收起详情」💠 Inline 语句</summary>
    <iframe src={"https://www.yuque.com/sacredcraft/kether/reference?view=doc_embed&inner=jdvIW"} width={'100%'} height={'500'}/>
</details>


先来介绍一下，这个语句可以让我们在文本中插入语句，插入语句的返回值会直接插入该位置变成文本的一部分。

为了能让系统从文本中识别我们插入的语句，我们需要将语句写到 `{{...}}` 里。



```ruby
/* 插入玩家名 */
tell inline "恭喜你 {{ player name }} 中奖啦！！奖池为VIVO50"


/* 插入变量 */
/* 制作血量显示 Action Bar */
set health to player health
set max-health to player max health
actionbar color inline "HP：&a&l{{ &health }} &f/ &c&l{{ &max-health }}"
```


<details>
  <summary>「展开 / 收起详情」🥝 演示效果</summary>

![](_images/10.webp)
![](_images/11.webp)

</details>

每一个内联脚本 `{{...}}` 在运行时都相当于去执行了一份新的 Kether 脚本，并在内联脚本执行后立刻获取结果替换文本中的内联脚本。

这意味着如果你在内联里使用 `Delay` 语句，那么将导致获取的结果为 `null`，我想应该没有人会这样写吧，不过还是想提醒一下各位。





此外，如果你只是想在一段文本里插入玩家的名字，很多时候都是可以不用 `inline` 的。

例如 `Tell`、`Broadcast`、`Action Bar`、`Command` 等语句都是支持将文本中的 `@sender` 字段替换成玩家的名字。（ 注意大小写 ）

```ruby
tell "欢迎光临 @sender"
command "kill @sender"
```





当然，除了 `inline`，你也可以用 `join` 来达到同样的效果，只不过看上去没有 `inline` 那么直观而已。

<details>
    <summary>「展开 / 收起详情」💠 Join 语句</summary>
    <iframe src={"https://www.yuque.com/sacredcraft/kether/reference?view=doc_embed&inner=Th01h"} width={'100%'} height={'500'}/>
</details>

`Join` 的作用是将多个语句的返回值拼凑成一段文本。

如果你的内联脚本正好处于文本的开头或末尾，那么用 `Join` 语句来代替 `Inline` 将会是个不错的主意。

```ruby
/* 对比：单个内联脚本 */
tell color inline "&7当前血量: {{ player health }}"
tell color join [ "&7当前血量" player health ]


/* 对比：多个内联脚本 */
set health to player health
set max-health to player max health
actionbar color inline "HP：&a&l{{ &health }} &f/ &c&l{{ &max-health }}"
actionbar color join [ "HP：&a&l" &health " &f/ &c&l" &max-health ]
```
