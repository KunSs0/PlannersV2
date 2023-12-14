package com.gitee.planners.api.common.script

import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.action.getEnvironmentContext
import com.google.common.collect.MultimapBuilder
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.kether.ExitStatus
import taboolib.module.kether.KetherShell
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface ComplexScriptPlatform {

    fun getCache(): KetherShell.Cache

    fun getRunningScripts(id: String, sender: Target<*>): List<ScriptContext>

    fun run(id: String, script: Script, options: KetherScriptOptions): CompletableFuture<Any>

    companion object {

        val SKILL = DefaultComplexScriptPlatform()

        // 配置重载时 刷新缓存
        @SubscribeEvent
        private fun onReload(e: PluginReloadEvents.Pre) {
            this.SKILL.getCache().scriptMap.clear()
        }

    }

    class DefaultComplexScriptPlatform : ComplexScriptPlatform {

        private val cache = KetherShell.Cache()

        private val runningScripts = MultimapBuilder.hashKeys().arrayListValues().build<String, ScriptContext>()

        override fun getRunningScripts(id: String, sender: Target<*>): List<ScriptContext> {
            val contexts = runningScripts[id].filter { !it.exitStatus.isPresent }
            return contexts.filter {
                it.rootFrame().getEnvironmentContext().sender == sender
            }
        }

        override fun getCache(): KetherShell.Cache {
            return cache
        }

        override fun run(id: String, script: Script, options: KetherScriptOptions): CompletableFuture<Any> {
            val build = options.build().build()
            return ScriptContext.create(script).also {
                runningScripts.put(id, it)
                if (build.sender != null) {
                    it.sender = build.sender
                }
                build.vars.map.forEach { (k, v) -> it.rootFrame().variables()[k] = v }
                build.context(it)
                it.rootFrame().addClosable(AutoCloseable {
                    runningScripts.remove(id, it)
                })
            }.runActions()
        }

    }

}
