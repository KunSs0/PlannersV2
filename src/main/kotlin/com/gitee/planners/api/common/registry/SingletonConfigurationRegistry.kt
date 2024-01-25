package com.gitee.planners.api.common.registry

import taboolib.common.platform.function.releaseResourceFile
import taboolib.common5.FileWatcher
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.mapSection

abstract class SingletonConfigurationRegistry<T : Unique>(val path: String) : AbstractConfigurationRegistry<T, ConfigurationSection>(),
    ReloadableRegistry {

    // 响应式
    open val responsive = false

    override fun onLoad() {
        val file = releaseResourceFile(path)

        if (responsive) {
            FileWatcher.INSTANCE.addSimpleListener(file, {
                this.table.clear()
                Configuration.loadFromFile(file).mapSection { this.load(it) }
                this.onLoaded()
            }, true)
        }
        // 非响应式
        else {
            this.table.clear()
            Configuration.loadFromFile(file).mapSection { this.load(it) }
            this.onLoaded()
        }
    }

    open fun onLoaded() {

    }

    override fun onReload() {
        if (!this.responsive) {
            this.onLoad()
        }
    }

}
