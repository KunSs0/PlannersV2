# 技能树分支图

技能树 graph 使用 section 表达分支，避免使用前置列表。一个节点下的 `groups` 是可选分支，分支之间为 OR；同一分支的 `requirements` 之间为 AND。

```yaml
graph:
  holy_emblem:
    groups:
      frontal:
        mode: ALL
        requirements:
          frontal_attack: 1
          steady_step: 3
      shield:
        mode: ALL
        requirements:
          shield_bash: 1
          defense_array: 3
```

`ALL` 分支要求全部前置节点达到等级；`ANY` 分支要求其中任意一个前置节点达到等级。根节点使用空的 `ALL` 分支：

```yaml
groups:
  root:
    mode: ALL
    requirements: {}
```

核心会同时生成两种视图：分支组用于购买校验，所有前置项展开为扁平 edge 列表，用于技能树快照的 `requirements` 和 UI 连线。生产 UI 通过每个 `nodeId` 找到布局坐标并绘制连线，因此不能只保留分支校验结构。
