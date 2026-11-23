package com.gitee.planners.module.action

/**
 * 技能动作注册入口。
 *
 * 在插件启用阶段调用一次：注册作用域。机制、目标器、条件与触发器的迁移按层推进，
 * 迁移完成前旧注册表仍然服务现有配置。
 */
object PlannersActionInstaller {

    private var installed = false

    /**
     * 安装技能动作扩展。
     *
     * @param owner 注册方，通常是插件实例
     */
    fun install(owner: Any) {
        if (installed) {
            return
        }
        installed = true
        PlannersActionScopes.install(owner)
    }

    /**
     * 重置安装标记，供测试隔离使用。
     */
    fun reset() {
        installed = false
    }
}
