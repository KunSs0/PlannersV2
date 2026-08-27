package com.gitee.planners.module.script

import com.novalang.runtime.Nova
import com.novalang.workspace.WorkspaceHost

/**
 * 向每个 Planners Nova 程序安装公开宿主能力的 Workspace Host。
 *
 * 本类型只负责安装 Planners 类加载器。公开 Java、Bukkit、Planners 与 Workspace API
 * 均由 Nova 源码通过 Java interop 直接调用，不在 Host 中重复包装。
 */
internal object PlannersWorkspaceHost : WorkspaceHost {

    /**
     * 将 Planners 宿主 API 安装到 Nova 程序。
     *
     * @param nova 当前 Workspace 正在构建的 Nova 程序。
     */
    override fun install(nova: Nova) {
        // Bukkit 与插件业务类必须通过 Planners 自身类加载器解析。
        nova.setScriptClassLoader(ScriptManager::class.java.classLoader)
    }
}
