package com.gitee.planners.api.common

import taboolib.common.LifeCycle
import taboolib.common.io.newFolder
import taboolib.common.platform.function.postpone
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration
import java.io.File

class DeepTrackRegistry<T : Unique>(
    val name: String,
    val attachs: List<String> = emptyList(),
    val invoke: Configuration.() -> T
) : AbstractRegistryBuiltin<T>() {

    private val folder = newFolder(name)

    init {
        init()
        // 在 LOAD 阶段 加载所有文件配置
        postpone(LifeCycle.LOAD) { this.load() }
    }

    fun init() {
        if (folder.exists()) {
            folder.mkdirs()
            attachs.forEach { releaseResourceFile("$name/$it") }
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

    override fun invokeInstance(config: Configuration): T {
        return invoke(config)
    }


}
