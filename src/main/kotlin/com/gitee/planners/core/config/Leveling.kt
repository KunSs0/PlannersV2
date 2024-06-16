package com.gitee.planners.core.config

import java.util.concurrent.CompletableFuture

interface Leveling {

    fun getLevel(): Int

    fun setLevel(level: Int)

    fun addLevel(value: Int)

    fun getExperience(): Int

    fun setExperience(experience: Int)

    fun addExperience(value: Int): CompletableFuture<Void>

    fun takeExperience(value: Int): CompletableFuture<Void>

}
