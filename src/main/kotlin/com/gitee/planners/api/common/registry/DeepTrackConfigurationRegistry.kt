package com.gitee.planners.api.common.registry

import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration
import java.io.File

abstract class DeepTrackConfigurationRegistry<T : Unique>(val name: String, private val attaches: List<String> = emptyList()) :
    AbstractConfigurationRegistry<T, Configuration>(), ReloadableRegistry {

    val folder by unsafeLazy { File(getDataFolder(), name) }

    open fun loadFromFile(file: File) {
        this.load(Configuration.loadFromFile(file))
    }

    override fun onLoad() {
        if (!folder.exists()) {
            folder.mkdirs()
            attaches.forEach { releaseResourceFile("$name/$it") }
        }
        this.folder.deepFiles().forEach { this.loadFromFile(it) }
    }

    override fun onReload() {
        this.table.clear()
        this.onLoad()
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

}
