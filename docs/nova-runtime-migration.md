# Nova 编译程序迁移

Planners 使用新版 `nova-runtime` 的 `NovaCompiler`、`CompiledNova` 和程序回调上下文，已移除 `nova-runtime-workspace` 依赖及 `WorkspaceHost`。

## 配置与入口

`script/nova.config.yml` 只维护 `version`、`name`、`aliases` 等编译元数据。公共启动入口由宿主明确指定为 `@planners/bootstrap`。旧的根级 `sources`、`entries`、`runtime` 配置必须删除；需要跨插件库时显式添加 `@nova` 别名。

技能 YAML 的完整 `action` 模块统一声明 `fun execute()`。`main` 保留给公共模块初始化；YAML 动作中声明 `main` 会拒绝加载并指出原始 YAML 来源，不提供旧入口兼容。

```nova
import "planners.core"

fun execute() {
    this.sender.sendMessage("技能等级 " + this.level)
}

fun onHit(payload) {
    val sender = this.sender
    val damage = this.damage
    // 继续调用实际的命中与结算 API。
}
```

状态的 `onLoad`、`onStateAttach`、`onStateDetach`、`onStateMount`、`onStateClose`、`onStateEnd` 及技能自定义回调名称保持其业务含义。调用绑定使用 `this.sender`、`this.ctx`、`this.level` 和 `this.变量ID`；函数形参与局部变量直接使用名称。class/object 方法所需业务数据通过参数传入。

## 生命周期

所有 YAML 虚拟源码在启动或重载阶段进入同一显式入口图，来源文件、节点和行号映射随源码保留。加载时初始化公共模块，技能只在施法或命中回调时执行。纯表达式继续通过显式参数调用，不在运行期隐式编译。

单次加载独占一个编译程序。业务调用绑定互相隔离，公共模块状态在当前程序内共享；Bukkit 业务入口由 Nova 宿主主线程执行。重载先关闭旧程序，再读取配置和编译新程序；关闭会取消其任务、注销资源并使旧回调失效。编译失败直接报告，不回滚旧程序。

## 验证

JDK 21 下执行 `./gradlew.bat --no-daemon build`。源码合成测试覆盖真实 Planners 编译入口的初始化、程序状态隔离、调用绑定隔离、主线程切换、任务取消、回调失效与旧 main 拒绝加载；默认资源测试保留全部 YAML 脚本的编译检查。

本服技能、参数与跨插件业务库位于 server-main，使用其 `scripts/Test-NovaRegressions.ps1 -Environment planners -Suite` 执行 Nova mock。迁移记录见服务端 `docs/nova-migration/planners-runtime-2026-10-16.md`。部署时使用同批新版 NovaLang 与 Planners，本文不代表已替换运行服 JAR 或已重启。
