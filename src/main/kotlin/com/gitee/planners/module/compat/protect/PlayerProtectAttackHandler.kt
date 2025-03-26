package com.gitee.planners.module.compat.protect

import taboolib.common.util.asList
import taboolib.common5.Demand
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.conversion

object PlayerProtectAttackHandler {

    @ConfigNode("settings.attack.protect.enable")
    val isAttackProtectEnable = true

    @ConfigNode("settings.attack.protect.scene")
    val attackProtectScenes = conversion<Any, List<Demand>> {
        this.asList().map { Demand(it) }
    }

    /**
     * 获取场景中的防护需求
     *
     * @param scene 场景名称
     *
     * @return 防护需求
     */
    fun getScene(scene: String): Demand? {
        return attackProtectScenes.get().firstOrNull { it.namespace == scene }
    }

    /**
     * 获取场景中的防护需求
     *
     * @param scene 场景名称
     *
     * @return 防护需求列表
     */
    fun getScenes(scene: String) : List<Demand> {
        return attackProtectScenes.get().filter { it.namespace == scene }
    }

    /**
     * 判断是否可以在场景中攻击
     *
     * @param scene 场景名称
     *
     * @return 是否可以攻击
     */
    fun canAttackOnScene(scene: String): Boolean {
        for (demand in attackProtectScenes.get()) {
            if (demand.namespace == scene) {
                return false
            }
        }

        return true
    }

}