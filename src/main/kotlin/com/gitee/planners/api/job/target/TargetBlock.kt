package com.gitee.planners.api.job.target

import org.bukkit.block.Block

class TargetBlock(val block: Block) : TargetBukkitLocation(block.location) {

    override fun toString(): String {
        return "TargetBlock(block=$block)"
    }

}
