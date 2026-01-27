package com.gitee.planners.api.job.target

class ProxyTargetContainer : ArrayList<ProxyTarget<*>>() {

    fun modified(func: (ProxyTarget<*>) -> ProxyTarget<*>) {
        this.forEachIndexed { index, target ->
            val newTarget = func(target)
            // 如果返回结果不一致 则直接更新对应容器目标
            if (newTarget != target) {
                this.set(index, newTarget)
            }
        }
    }

    companion object {

        fun of(vararg target: ProxyTarget<*>): ProxyTargetContainer {
            return ProxyTargetContainer().also {
                it += target
            }
        }
    }
}
