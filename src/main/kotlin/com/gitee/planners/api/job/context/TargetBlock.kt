package com.gitee.planners.api.job.context

import org.bukkit.Location
import org.bukkit.block.Block

class TargetBlock(val block: Block) : TargetBukkitLocation(block.location)
