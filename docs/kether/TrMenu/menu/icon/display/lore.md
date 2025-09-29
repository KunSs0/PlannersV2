---
title: 描述
sidebar_position: 3
---

import { Admonition } from '@site/src/components/GitBook';

# 描述

物品的 Lore 描述

## 示例

```yaml
# 嵌套 List，多组动态 Lore
lores:
  - - '&7Thanks &f:> &7for using!'
  - - '&7Thanks &f:) &7for using!'

# 静态 Lore 描述
lore:
  - 'Hello There'
  - 'Hello TrMenu!'
```

## 条件

<Admonition type="info">
此功能将在每次更新 Lore 时进行计算条件，可能影响插件性能表现
</Admonition>

```yaml
lore:
  - 'Line 1'
  - 'Line 2'
  # 当玩家有 vip.user 权限时显示此行
  - 'You are VIP User {condition=perm *vip.user}'
  - 'Line 3'
```



