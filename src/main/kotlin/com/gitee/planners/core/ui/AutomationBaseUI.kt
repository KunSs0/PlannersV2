package com.gitee.planners.core.ui

import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common.platform.function.warning
import taboolib.common5.FileWatcher
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.reflex.ClassField
import taboolib.library.xseries.XItemStack
import taboolib.module.configuration.*
import taboolib.module.ui.type.Basic
import taboolib.module.ui.type.Linked
import java.util.function.Supplier

abstract class AutomationBaseUI(name: String) : BaseUI {

    private val path = "ui/$name"

    private lateinit var config: Configuration

    class SimpleConfigNodeTransfer<T, V>(private val block: T.() -> V) : Supplier<V> {

        private var value: V? = null

        @Suppress("UNCHECKED_CAST")
        fun update(value: Any) {
            this.value = this.block(value as T)
        }

        override fun get(): V {
            return value ?: error("Unable to get value")
        }

    }

    companion object {

        fun <T, V> simpleConfigNodeTo(block: T.() -> V): SimpleConfigNodeTransfer<T, V> {
            return SimpleConfigNodeTransfer { block(this) }
        }

        fun decorateIcon() = simpleConfigNodeTo<Configuration, List<DecorateIcon>> {
            this.getKeys(false).filter { it != "__option__" }.map {
                DecorateIcon(this.getConfigurationSection(it)!!)
            }
        }

        fun Basic.injectDecorateIcon(icons: List<DecorateIcon>) {
            icons.forEach { icon ->
                icon.slots.forEach {
                    this.set(it, icon.icon)
                }
            }
        }

    }

    class DecorateIcon(config: ConfigurationSection) {

        val slots = if (config.isInt("slot")) {
            listOf(config.getInt("slot"))
        } else {
            config.getIntegerList("slots")
        }

        val icon = XItemStack.deserialize(config)

    }

    @Awake
    class OptionVisitor : ClassVisitor(2) {
        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.INIT
        }

        override fun visit(field: ClassField, clazz: Class<*>, instance: Supplier<*>?) {
            if (AutomationBaseUI::class.java.isAssignableFrom(clazz) && field.isAnnotationPresent(BaseUI.Option::class.java)) {
                val automationBaseUI = instance?.get() as? AutomationBaseUI ?: return
                val option = field.getAnnotation(BaseUI.Option::class.java)
                val bind = automationBaseUI.path
                println("====== bind $bind node ${option.property<String>("node")}")
                val file = ConfigLoader.files[bind]
                if (file == null) {
                    warning("$bind not defined")
                    return
                }
                file.nodes += field
                val node = option.property("node", "")

                if (node == "*" && field.fieldType == SimpleConfigNodeTransfer::class.java) {
                    val transfer = field.get(instance.get()) as SimpleConfigNodeTransfer<*, *>
                    transfer.update(file.configuration)
                    return
                }

                val data = file.configuration[node.ifEmpty { field.name.toNode() }]
                if (data == null) {
                    warning("$node not found in $bind")
                    return
                }
                if (field.fieldType == SimpleConfigNodeTransfer::class.java) {
                    val transfer = field.get(instance.get()) as SimpleConfigNodeTransfer<*, *>
                    transfer.update(data)
                } else {
                    field.set(instance.get(), data)
                }
            }
        }

        fun String.toNode(): String {
            return map { if (it.isUpperCase()) "-${it.lowercase()}" else it }.joinToString("")
        }

    }

    @Awake
    class ConfigVisitor : ClassVisitor(1) {

        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.INIT
        }

        override fun visitEnd(clazz: Class<*>, instance: Supplier<*>?) {
            if (BaseUI::class.java.isAssignableFrom(clazz) && instance != null) {
                val automationBaseUI = instance.get() as? AutomationBaseUI ?: return
                val path = automationBaseUI.path
                if (ConfigLoader.files.containsKey(path)) {
                    automationBaseUI.config = ConfigLoader.files[path]!!.configuration
                } else {
                    val file = releaseResourceFile(path)
                    val conf = Configuration.loadFromFile(file)
                    automationBaseUI.config = conf
                    // 自动重载文件
                    FileWatcher.INSTANCE.addSimpleListener(file) {
                        if (file.exists()) {
                            conf.loadFromFile(file)
                        }
                    }
                    val nodeFile = ConfigNodeFile(conf, file)
                    // 自动重载节点
                    conf.onReload {
                        val loader = PlatformFactory.getAPI<OptionVisitor>()
                        nodeFile.nodes.forEach { loader.visit(it, clazz, instance) }
                    }
                    ConfigLoader.files[path] = nodeFile
                }
            }
        }

    }

}
