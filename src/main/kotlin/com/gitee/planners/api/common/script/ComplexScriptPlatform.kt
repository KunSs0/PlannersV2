package com.gitee.planners.api.common.script

import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.action.getEnvironmentContext
import com.google.common.collect.MultimapBuilder
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.kether.Quest
import taboolib.library.kether.Quest.Block
import taboolib.library.kether.QuestContext
import taboolib.module.kether.KetherShell
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import java.util.Optional
import java.util.concurrent.CompletableFuture

interface ComplexScriptPlatform {

    fun getCache(): KetherShell.Cache

    fun getBlock(source: String,block: String) : Optional<Quest.Block>

    fun getRunningScripts(id: String, sender: Target<*>): List<ScriptContext>

    fun run(id: String,script: Script,block: Quest.Block,options: KetherScriptOptions)

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

        override fun getBlock(source: String, block: String): Optional<Quest.Block> {
            val quest = cache.scriptMap[source]
            if (quest != null) {
                return quest.getBlock(block)
            }
            return Optional.empty()
        }

        override fun run(id: String, script: Script, block: Block, options: KetherScriptOptions) {
            val context = createScriptContext(id, script, options)
            block.actions.forEach { action ->
                action.process(context.rootFrame())
            }
        }

        private fun createScriptContext(id: String,script: Script,options: KetherScriptOptions): QuestContext {
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
            }
        }

        override fun run(id: String, script: Script, options: KetherScriptOptions): CompletableFuture<Any> {
            return createScriptContext(id, script, options).runActions()
        }

    }

}
