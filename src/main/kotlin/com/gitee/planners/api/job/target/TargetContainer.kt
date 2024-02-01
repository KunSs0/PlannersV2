package com.gitee.planners.api.job.target

class TargetContainer : ArrayList<Target<*>>() {


    fun modified(func: (Target<*>) -> Target<*>) {
        this.forEachIndexed { index, target ->
            val newTarget = func(target)
            // 如果返回结果不一致 则直接更新对应容器目标
            if (newTarget != target) {
                this.set(index,newTarget)
            }
        }
    }

    companion object {

        fun of(vararg target: Target<*>): TargetContainer {
            return TargetContainer().also {
                it += target
            }
        }


    }

}
