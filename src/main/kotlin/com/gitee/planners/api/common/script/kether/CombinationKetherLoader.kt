package com.gitee.planners.api.common.script.kether

import com.gitee.planners.api.common.Plugin
import com.gitee.planners.util.RunningClassRegistriesVisitor.Companion.toClass
import com.gitee.planners.util.checkPlugin
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.function.warning
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass
import taboolib.module.kether.printKetherErrorMessage
import java.util.function.Supplier


@Awake
class Visitor : ClassVisitor(0) {

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.LOAD
    }

    override fun visit(method: ClassMethod, owner: ReflexClass) {
        if (method.isAnnotationPresent(CombinationKetherParser.Used::class.java) && CombinationKetherParser::class.java.isAssignableFrom(method.returnType)) {
            if (method.isAnnotationPresent(CombinationKetherParser.Ignore::class.java)) {
                return
            }
            try {
                val instance = owner.getInstance()
                val combinationKetherParser = (if (instance == null) {
                    method.invokeStatic()
                } else {
                    method.invoke(instance)
                }) as CombinationKetherParser

                KetherHelper.registerCombinationKetherParser(method.name, combinationKetherParser)
            }catch (e: Exception) {
                warning("Error by ${owner.toClass()}#${method.name}")
                e.printKetherErrorMessage(false)
            }
        }
    }

    override fun visitEnd(clazz: ReflexClass) {
        if (clazz.hasAnnotation(CombinationKetherParser.Used::class.java) && CombinationKetherParser::class.java.isAssignableFrom(clazz.toClass())) {
            if (clazz.hasAnnotation(CombinationKetherParser.Ignore::class.java)) {
                return
            }

            // 检查前置
            if (clazz.hasAnnotation(Plugin::class.java) && !checkPlugin(clazz.getAnnotation(Plugin::class.java).property("name")!!)) {
                return
            }
            val instance = clazz.getInstance()

            val combinationKetherParser = instance as? CombinationKetherParser ?: return
            KetherHelper.registerCombinationKetherParser(combinationKetherParser)
        }
    }



}

