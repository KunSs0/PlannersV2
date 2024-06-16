package com.gitee.planners.api.job

import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.common.script.KetherScriptOptions
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface Route : Unique {

    // 得到当前分支下的所有分支
    fun getBranches(): List<Route>

    fun getJob() : Job

    fun getIcon() : ItemStack?

    fun isInfer(player: Player,options: KetherScriptOptions): Condition.VerifyInfo

}
