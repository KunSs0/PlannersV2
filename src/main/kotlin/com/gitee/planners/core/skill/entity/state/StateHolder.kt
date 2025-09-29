package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.core.config.State

class StateHolder(val entity: TargetEntity<*>, val state: State) {

    companion object {

        // 初始状态
        const val LIFE_INIT = 0;

        // 等待状态
        const val LIFE_WAITING = 1;

        // 结束状态
        const val LIFE_END = 2;
    }

    // 实体当前状态
    var life = LIFE_INIT

}