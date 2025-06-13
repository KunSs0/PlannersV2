package com.gitee.planners.core.ui

import com.gitee.planners.util.Reflex
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common.platform.function.warning
import taboolib.common5.FileWatcher
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.reflex.ClassField
import taboolib.library.reflex.ReflexClass
import taboolib.library.xseries.XItemStack
import taboolib.module.configuration.*
import java.util.function.Supplier

abstract class AutomationBaseUI(name: String) : BaseUI {

    private val path = "ui/$name"

    private lateinit var config: Configuration

    @Option("__option__.rows")
    val rows = 6

    @Option("__option__.title")
    val title = "Chest"

    @Option("*")
    val decorateIcon = decorateIcon()

    override fun openTo(player: Player) {
        player.openInventory(setDecorateIcon(decorateIcon.get(), display(player).build()))
    }

    fun decorateIcon() = simpleConfigNodeTo<Configuration, List<Icon>> {
        this.getKeys(false).filter { it != "__option__" }.map {
            Icon(this.getConfigurationSection(it)!!)
        }
    }

    fun setDecorateIcon(icons: List<Icon>, inventory: Inventory): Inventory {
        icons.forEach { icon ->
            icon.slots.forEach {
                inventory.setItem(it, icon.icon)
            }
        }
        return inventory
    }

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Option(val node: String)

    /**
     * 隔离转换器 与taboolib的transfer隔离
     */
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

    }

    class Icon(config: ConfigurationSection) {

        val slots = if (config.isInt("slot")) {
            listOf(config.getInt("slot"))
        } else {
            config.getIntegerList("slots")
        }

        val icon = XItemStack.deserialize(config)

    }

    @Awake
    class ConfigVisitor : ClassVisitor(1) {

        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.INIT
        }

        override fun visitEnd(clazz: ReflexClass) {
            if (BaseUI::class.java.isAssignableFrom(clazz.toClass()) && clazz.getInstance() != null) {
                val automationBaseUI = clazz.getInstance() as? AutomationBaseUI ?: return
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
                        nodeFile.nodes.forEach { visitOption(it, clazz.toClass()) { clazz.getInstance() } }
                    }
                    ConfigLoader.files[path] = nodeFile
                }
                Reflex.getFieldsWithSuperclass(clazz.toClass()).forEach { field ->
                    visitOption(field, clazz.toClass()) { clazz.getInstance() }
                }
            }
        }


        fun visitOption(field: ClassField, clazz: Class<*>, instance: Supplier<*>?) {
            if (AutomationBaseUI::class.java.isAssignableFrom(clazz) && field.isAnnotationPresent(Option::class.java)) {
                val automationBaseUI = instance?.get() as? AutomationBaseUI ?: return
                val option = field.getAnnotation(Option::class.java)
                val bind = automationBaseUI.path
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

}
