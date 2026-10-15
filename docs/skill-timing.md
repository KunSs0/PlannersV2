# 技能冷却与施法计时

完整释放入口 `PlannersAPI.cast(player, PlayerSkill)` 在开始时捕获一次属性时间收益，生成唯一时间快照。指向确认、施法等待、动画Hook和最终资源校验共用这份快照；低层直接运行脚本接口仍不自动等待或提交冷却。

全局配置必须明确指定来源和取整策略：

```yaml
settings:
  skill:
    timing:
      provider: unmodified
      tick-rounding: ceil
```

`unmodified`明确选择原时间；外部Provider ID必须已注册，缺失直接报错，不采用原时间回退。取整支持ceil、floor、nearest（半数取偶），没有源码默认选项。FightCore集成使用 `provider: fightcore`。

所有技能都必须在 `__option__.variables` 中声明 `castTime`，单位tick；瞬发和被动技能明确配置0。变量值可以由已有Nova表达式计算，缺项不提供兼容。基础时间必须是整数范围内的非负有限数。

```yaml
__option__:
  name: "计时示例"
  category: [active]
  variables:
    castTime: 40
    cooldown: 200
```

```text
最终冷却tick = 按配置取整(基础cooldown × (1 - 冷却缩短比例))
最终施法tick = 按配置取整(基础castTime × (1 - 施法缩短比例))
```

外部实现 `SkillTimingProvider.capture(player)`，返回经过范围校验的 `SkillTimingModifiers`。通过 `SkillTimingProviders.register(id, owner, provider)` 注册，原所有者负责注销。Planners不复制外部属性公式。

释放顺序：Check → 基础变量与时间快照 → 初次资源校验 → 指向确认（若有） → Pre → 最终施法tick等待 → 原动画Hook → 最终资源校验 → 冷却和资源消耗 → action → Post。缩短施法时间不会改变动作动画速度。

第三方释放条件在初次校验后按显式START配置支付开始成本；COMMIT成本与自动冷却仍在施法等待、Hook及最终校验后提交。取消不会退还已支付的START成本。等待期间重复释放被拒绝。取消、退出、死亡、配置重载及计时宿主关闭后，旧回调不会继续提交COMMIT成本或自动冷却。显式0施法立即继续，不额外等待一个tick。没有新增移动或受伤自动打断规则，也没有新增客户端读条UI。业务可调用 `SkillCastingSessions.cancel(player)` 执行明确的中断规则。

`SkillInputExec.Context.resume()`同次释放只能执行一次。action绑定的cooldown和castTime均为本次最终tick，普通技能action不要重复调用Cooler.set重置自动冷却；底层Cooler.set仍接受最终tick供管理重置等用途，不再次应用属性收益。图标与基础变量查询仍表示基础时间。

FightCore与Planners源码需配套构建，服务端技能配置同步迁移后再配套部署。当前已有技能明确迁移为castTime=0，没有给它们添加未策划的读条时长。
