package com.gitee.planners.api.common

import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.io.newFile
import taboolib.common.io.newFolder
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.postpone
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration
import java.io.File
import java.util.function.Supplier

abstract class DeepTrackRegistry<T : Unique>(val name: String, private val attaches: List<String> = emptyList()) : AbstractRegistryBuiltin<T>() {

    val folder by unsafeLazy { File(getDataFolder(), name) }

    init { register(this) }

    fun init() {
        if (!folder.exists()) {
            folder.mkdirs()
            attaches.forEach { releaseResourceFile("$name/$it") }
        }
    }

    fun load() {
        this.folder.deepFiles().forEach { this.loadFromFile(it) }
    }

    fun reload() {
        this.table.clear()
        this.load()
    }

    private fun File.deepFiles(): List<File> {
        val files = mutableListOf<File>()
        when {
            this.extension == "yml" -> {
                files += this
            }

            this.isDirectory -> {
                files += this.listFiles()?.flatMap { it.deepFiles() } ?: emptyList()
            }
        }
        return files
    }

    companion object {

        private val registry = mutableListOf<DeepTrackRegistry<*>>()

        fun register(deepTrackRegistry: DeepTrackRegistry<*>) {
            this.registry += deepTrackRegistry
        }

        @Awake(LifeCycle.LOAD)
        internal fun onLoad() {
            println("on load $registry")
            registry.forEach { it.init() }
        }

        fun onReload() {
            this.registry.forEach { it.reload() }
        }

        @Awake(LifeCycle.ENABLE)
        internal fun onEnable() {
            println("on enable $registry")
            registry.forEach { it.load() }
        }

    }

}
