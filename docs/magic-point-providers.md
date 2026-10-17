# 法力提供器生命周期与资源变更

`MagicPointProvider.INSTANCE` 在 Bukkit 主线程切换提供器。赋值前先失效旧所有权并调用旧实例的 `close()`，关闭失败则不安装新实例；同一实例重复赋值幂等。`unregister(provider)` 仅释放当前所有者，Planners 停止也使用该入口，不自动恢复默认提供器。

`close()` 是提供器的资源清理钩子。原生 `DefaultMagicPointProvider` 先关闭执行入口，再取消上限与回复两项周期任务；配置 reload 不会重建已关闭任务。宿主已经取出的旧回调也会在入口拒绝执行。原生任务始终操作自身元数据，不通过全局实例转发到外部资源。

`getPoint()` 是整数技能门槛和展示投影，`setPoint()` 表示显式设置整数目标。相对变更使用 `changePoint(player, delta)`，实际技能扣费使用 `consumePoint(player, amount)`。外部资源系统覆盖这两个扩展点，直接在权威存储中操作；不得读取整数展示值后将其写回 Double 存储。

`PlayerTemplateAPI.addMagicPoint/takeMagicPoint` 在事件通过后调用相对变更接口，并采用事件最终的 amount。技能 mp 条件调用扣费接口，真正扣费时重新检查余额；失败不写入资源。原生提供器继续使用其已有整数法力语义，FightCore 接管后唯一的自然回复所有者为 FightCore。

JDK 21 执行 `gradlew.bat --no-daemon build --console=plain`。`MagicPointProviderTest` 验证两项任务取消、关闭后的 reload 与在途回调、重复接入、启动顺序、所有者卸载，以及实际技能/API 变更接口。
