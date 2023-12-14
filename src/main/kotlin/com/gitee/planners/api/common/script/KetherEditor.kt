package com.gitee.planners.api.common.script

import kotlin.reflect.KClass

annotation class KetherEditor {

    /**
     * 编辑器设计规范
     * namespace identify
     * <> 必选
     * [] 可选
     * token 前缀解析
     * [token var:(默认)] 可选(前缀 变量:(默认值))
     * [objective: (sender/console/origin/empty)] 可选目标容器：默认配置
     * (~namespace)
     */
    annotation class Document(val value: String, val result: KClass<*> = Void::class, val author: String = "yoyo")


}
