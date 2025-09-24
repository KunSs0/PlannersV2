package com.gitee.planners.core.skill.script

import com.gitee.planners.api.PlayerTemplateAPI.plannersLoaded
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.module.kether.context.CompiledScriptContext
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import org.bukkit.entity.Player
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.library.kether.Quest
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptOptions
import taboolib.module.kether.runKether

open class ScriptCallback(
    val id: String,
    val compiled: ComplexCompiledScript,
    val ignoreCancelled: Boolean,
    val priority: EventPriority,
    val block: Quest.Block?,
    val async: Boolean
) {

    // 是否已被关闭
    var closed: Boolean = false

    lateinit var mapping: ProxyListener

    /**
     * 调用脚本
     *
     * @param sender 触发者
     * @param event 事件
     * @param holder 事件持有者
     */
    open fun <T> call(sender: Target<*>, event: T, holder: ScriptEventHolder<T>) {
        if (this.closed) {
            return
        }
        // 对技能做出处理
        val ctx = if (compiled is ImmutableSkill) {
            // 如果是玩家 则转为技能释放上下文
            val level =
                if (sender is TargetBukkitEntity && sender.instance is Player && sender.instance.plannersLoaded) {
                    sender.instance.plannersTemplate.getRegisteredSkillOrNull(compiled.id)?.level ?: 0
                } else {
                    0
                }
            ImmutableSkillContext(sender, compiled, level)
        } else {
            CompiledScriptContext(sender, compiled)
        }

        runKether {
            // 关联异步逻辑
            ctx.async = this.async
            // 在event运行环境下，如果非异步行为，强制设置为now
            if (!ctx.async) {
                ctx.now = true
            }

            // 构建脚本执行环境
            val block: ScriptOptions.ScriptOptionsBuilder.() -> Unit = {
                context {
                    whenBegin(sender, event, holder, this)
                }
            }

            if (this.block == null) {
                ctx.call(block)
            } else {
                ctx.call(this.block, block)
            }


        }
    }

    /**
     * 脚本开始执行
     *
     * @param sender 触发者
     * @param event 事件
     * @param holder 事件持有者
     * @param ctx 脚本上下文
     */
    open fun <T> whenBegin(sender: Target<*>, event: T, holder: ScriptEventHolder<T>, ctx: ScriptContext) {
        ctx["id"] = this.id
        holder.handle(event, ctx)
    }

}
