package com.gitee.planners

import com.gitee.action.api.Action
import com.gitee.action.engine.ActionServicesImpl

/**
 * 单测用的 Action 引导。
 *
 * 桥接契约需要真实引擎才能验证（服务集合、注册表与执行派发都在引擎侧），
 * 因此单测引入 Action 运行期包；同一 JVM 只安装一次。
 */
object ActionTestBootstrap {

    /**
     * 确保 Action 服务集合已安装。
     */
    @Synchronized
    fun ensureInstalled() {
        if (Action.isInstalled()) {
            return
        }
        Action.install(ActionServicesImpl())
    }
}
