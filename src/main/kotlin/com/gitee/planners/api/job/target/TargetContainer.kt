package com.gitee.planners.api.job.target

class TargetContainer : ArrayList<Target<*>>() {

    companion object {

        fun of(vararg target: Target<*>): TargetContainer {
            return TargetContainer().also {
                it += target
            }
        }


    }

}
