# Zeus 技能树快照脚本性能优化方案

> 状态：**方案文档，未应用到生产**。测试仓库（planners-v2）已完成全部改造并通过
> mock 验证；生产环境（server-main 侧脚本与 ScriptEngine libs）需按本方案手动同步。
>
> 数据来源：`SkillTreeBukkitPlannersAccessScenarioTest` /
> `SkillTreeSnapshotScenarioTest`（MockBukkit + 生产 zeus.planners.js 脚本，
> 42 节点 / 13 技能 / 87 条件规模）。

## 一、背景

Zeus 职业界面的技能树快照由 ScriptEngine 执行 `zeus.planners.js` 生成。
优化前基线（批量投影方案）单次快照约 **1.63 ms**；为支持更自然的脚本业务写法，
将预合并投影路径拆除、改为逐节点反查后恶化至 **4 ms+**。本轮优化的目标是：
在保持"JS 循环反查"真实业务语义的前提下，把耗时压回并低于原基线。

## 二、优化总览

| # | 优化项 | 类型 | 改动位置 |
|---|---|---|---|
| 1 | 拆除批量投影（IntArray/BooleanArray），改为逐节点反查 | 结构调整 | zeus.planners.js |
| 2 | 宿主对象代理层（ProxyObject + ProxyExecutable） | 宿主层 | PlayerRoutePolyglotView.kt |
| 3 | 配置级缓存统一并入 PlannersJs.registry | 脚本层 | zeus.planners.js + registry 实现 |
| 4 | registeredSkills 注册表缓存 | 脚本层 | zeus.planners.js |
| 5 | hints 去掉 toArray 拷贝 | 脚本层 | zeus.planners.js |

## 三、各项详情

### 1. 拆除批量投影

**原方案**：宿主侧 `PlayerRoute.getSkillTreeRuntimeProjection()` 将全部节点的
等级（IntArray）、可激活状态（BooleanArray）、提示（List<List<String>>）
预先合并成投影对象一次性传给 JS。

**问题**：IntArray/BooleanArray 不是真实业务模型；脚本被迫消费预合并数组，
无法表达"查某个节点状态"这类自然业务查询。

**新方案**：删除 `getSkillTreeRuntimeProjection` 与 `SkillTreeRuntimeProjection`
类。脚本按业务粒度逐节点反查：

```js
for (var i = 0; i < view.getNodeCount(treeId); i++) {
    var nodeId = view.getNodeIdAt(treeId, i);
    var level   = route.getNodeLevelById(treeId, nodeId);
    var canDo   = route.isNodeCanAdvance(treeId, nodeId);
    var hints   = route.getNodeHints(treeId, nodeId);   // 数组语义
}
```

### 2. 宿主对象代理层（核心）

裸 Kotlin 对象的反射式方法分派很贵：实测逐节点反查稳态 **5.8 ms**，
是批量投影（1.7 ms）的 3.4 倍。

新增 `PlayerRoutePolyglotView`（src/main/kotlin/com/gitee/planners/core/player/）：

- 实现 `org.graalvm.polyglot.proxy.ProxyObject`
- 全部业务方法以预构建的 `ProxyExecutable` 闭包暴露——绕开 GraalJS 的
  反射成员解析，JS 调用方式完全不变
- 树/节点索引 O(1)：初始化时构建 treeId→树、treeId→(nodeId→下标) 两级 HashMap
- 同一请求内 canAdvance/hints 结果缓存（玩家未操作时节点状态不变）
- `getNodeHints` 返回 `StringListProxy : ProxyArray`，JS 按 `.length/[i]`
  原生数组语义直读

配套接口 `RealLookupTarget`（最小业务查询集）+ `RealLookupTreeDefinition`：
生产 `PlayerRoute` 与测试 Mock 共同实现，保证脚本走的是同一套代理逻辑。

入口：`PlayerRoute.getSkillTreeRuntimeProjectionView(player): Any`。

**效果**：逐节点反查稳态 5.81 → 2.80 ms（微基准）；冷启动快 6.3 倍。

### 3. 配置级缓存并入 PlannersJs.registry

**原则**：注册表与配置统一归 `PlannersJs.registry`，不单独建配置文件；
无版本号机制——脚本 reload 后上下文整体重建，缓存自然失效。

registry 新增成员：

```js
PlannersJs.registry.cached(namespace, key, builder)
// namespace+key 命中即返回；否则执行 builder 存入
```

收编的缓存点（原散落在脚本里的手写 cache 变量全部删除）：

| namespace | 内容 | key |
|---|---|---|
| tree | 技能树静态结构（节点定义/坐标/前置） | treeId |
| router | 路由结构（职业/分支/技能树清单） | routerId |
| backpack | 背包结构（页面/槽位/按键名） | "default" |
| skillDisplay | 技能图标展示数据（名称/lore/分类） | skillId:level |
| registeredSkills | 玩家已注册技能等级表 | bindingId |

示例（技能图标数据）：

```js
var data = PlannersJs.registry.cached(
    "skillDisplay",
    skillId + ":" + String(level),
    function () {
        var renderedIcon = DynamicSkillIcon.render(player, skill, Number(level));
        return { id: skillId, name: ..., displayIconName: ..., displayIconLore: ... };
    }
);
```

**效果**：immutable 阶段 1.33 → 0.27 ms；13 个技能图标的渲染成本从
"每玩家每请求"降为全局一次。

### 4. registeredSkills 注册表缓存

`toPlayerJobData` 中对 `route.getRegisteredSkill()` 的 entrySet 迭代
（13 entry × 3 次穿越 ≈ 39 次）改为整表缓存：

```js
var allSkills = PlannersJs.registry.cached(
    "registeredSkills",
    String(route.getBindingId()),
    function () {
        var result = {};
        var it = route.getRegisteredSkill().entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            result[String(e.getKey())] = Number(e.getValue().getLevel());
        }
        return result;
    }
);
var skills = {};
for (var cachedSkillId in allSkills) {
    if (includedSkillIds == null || includedSkillIds[cachedSkillId] === true) {
        skills[cachedSkillId] = { level: allSkills[cachedSkillId] };
    }
}
```

约束：**缓存的 skills 对象跨请求共享，必须只读**。includedSkillIds 过滤在
缓存之上按需构建，不得写回。

已知取舍：技能升级不会主动失效该缓存（当前设计接受 reload 前显示旧等级；
如需精确失效，后续可在升级事件中调用 registry 清理对应命名空间）。

### 5. hints 去 toArray 拷贝

`toPlayerTreeData` 中 `PlannersJs.convert.toArray(route.getNodeHints(...))`
改为直接传递代理数组（ProxyArray 具备 `.length/[i]` 与 JSON.stringify 语义）。
配合第 2 项的 StringListProxy 生效。

## 四、优化前后数据

### 管线分段（稳态预热后，单位 ms）

| 阶段 | 优化前 | 优化后 |
|---|---|---|
| backpack 背包收集 | 0.98 | 0.29 |
| immutable 技能数据+图标渲染 | 1.33 | 0.27 |
| playerJobs 职业/树/节点组装 | 2.38 | **0.70** |
| 纯节点反查环（42 节点×4 查询） | 0.71 | 0.33 |
| **完整快照（含 JSON 序列化）** | **7.31** | **3.29~5.89** |

### 关键场景对照

| 场景 | 优化前 | 优化后 |
|---|---|---|
| 生产脚本完整访问链路（payload 17633 字符） | 2.12 ms/次 | **1.55 ms/次（-27%）** |
| 42 节点快照·代理层反查（预热后） | — | 1.42~2.21 ms |
| 42 节点快照·批量投影（历史基线参照） | 0.86 ms | 已拆除 |

### 冷启动说明

服务器启动后首个触发快照的请求承担 JIT/Truffle 首译成本（完整快照首遍
30~47 ms），第 5 轮起进入稳态。如需消除，可在插件启用完成后执行一次
预热快照。

## 五、高并发评估

- GraalJS Context 非线程安全，SHARED_ENGINE 下所有脚本请求串行过锁
- 稳态 ~2 ms/次 → 单上下文理论上限 **~500 QPS**；排队延迟随并发线性叠加
- tick 级缓存（`getSkillTreeSnapshot` 带 cacheHit）已保证同一玩家同一 tick
  内只算一次，实际压力取决于"每 tick 不同玩家数"

## 六、生产同步清单（server-main 侧）

1. `ScriptEngine/libs` 的 registry 宿主实现补 `cached(namespace, key, builder)`
   方法（Kotlin 侧 builder 参数声明为 `org.graalvm.polyglot.Value`，
   直接 `.execute()`）
2. `zeus.planners.js` 同步改动：
   - toPlayerTreeData 改为 `(route, tree, context)` 逐节点反查签名
   - toPlayerJobData 的 skills 构建改走 registry.cached + 过滤
   - getTreeStructure/getRouterStructure/getBackpackStructure/
     toImmutableSkillData 切换 registry.cached
3. planners-v2 测试资源目录 `src/test/resources/zeus-script/` 为权威参考实现
4. 若生产存在独立 api.utils.js，无需改动（toArray 未变更）

## 七、遗留事项

- `SkillTreeRuntimeProjection` 类及批量投影入口已在测试仓库删除；
  生产同步时一并清理
- Context 池化（多实例分摊并发）未实施，属 ScriptManager 层结构改动，
  如 QPS 不满足预期再立项
