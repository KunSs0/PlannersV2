package com.gitee.planners.api.event.player

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetContainer
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class TargetCapturedEvent(
    val sender: Target<*>,
    val container: TargetContainer,
    val cause: Cause
) : BukkitProxyEvent() {

    companion object {

        /* 玩家被攻击 */
        val DAMAGED = Cause("damaged")

        fun damaged(sender: Target<*>, container: TargetContainer) = call(sender, container, DAMAGED)

        /**
         * 快速调用事件，将返回一个被处理后的对象容器
         *
         * @param sender 发送者
         * @param container 对象容器
         * @param cause 原因
         *
         * @return 被处理后的对象容器
         */
        fun call(sender: Target<*>, container: TargetContainer, cause: Cause): TargetContainer {
            if (container.any { it.instance is Player }) {
                val event = TargetCapturedEvent(sender, container, cause)
                // 如果事件被取消，则代表本次不允许被捕获
                if (!event.call()) {
                    event.container.clear()
                }

                return event.container
            }


            return container
        }

    }

    class Cause(val name: String)

}
