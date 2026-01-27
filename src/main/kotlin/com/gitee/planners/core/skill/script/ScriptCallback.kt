package com.gitee.planners.core.skill.script

import com.gitee.planners.api.PlayerTemplateAPI.plannersLoaded
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import org.bukkit.entity.Player
import org.bukkit.event.Event
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import java.util.concurrent.CompletableFuture

open class ScriptCallback<T>(
    val id: String,
    val script: SingletonFluxonScript,
    val ignoreCancelled: Boolean,
    val priority: EventPriority,
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
    open fun call(sender: ProxyTarget<*>, event: T, holder: ScriptEventHolder<T>): CompletableFuture<Any?> {
        if (this.closed) {
            return CompletableFuture.completedFuture(null)
        }

        val options = FluxonScriptOptions.create {
            set("sender", sender.instance)
            set("id", this@ScriptCallback.id)
            async(this@ScriptCallback.async)
        }

        // 如果 sender 是玩家且关联技能
        if (sender is ProxyTarget.BukkitEntity && sender.instance is Player && sender.instance.plannersLoaded) {
            options.set("player", sender.instance)
        }

        // 注入事件变量
        if (event is Event) {
            holder.handle(event as T, options)
        }

        return script.run(options)
    }

}
