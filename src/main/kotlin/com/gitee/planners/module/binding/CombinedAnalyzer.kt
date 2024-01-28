package com.gitee.planners.module.binding

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.common.registry.AbstractRegistry
import com.gitee.planners.api.common.registry.defaultRegistry
import com.gitee.planners.api.event.action.CombinedEvent
import com.gitee.planners.api.job.KeyBinding
import org.bukkit.entity.Player

class CombinedAnalyzer {

    val inferences = mutableListOf<Inference>()

    fun process(action: InteractionAction): List<Inference> {
        // 删除已经超时的推断
        inferences.removeIf { it.isClosedWithRequestTick() }
        return this.inferences.filter { it.pressIn(action) }
    }

    fun registerCombined(combined: Combined) {
        this.inferences += createInference(combined)
    }

    companion object {

        private val builtin = defaultRegistry<Player, CombinedAnalyzer>()

        fun processAction(player: Player,type: InteractionActionBukkitType) {
            processAction(player,InteractionActionBukkitToken(type))
        }

        fun processAction(player: Player, action: InteractionAction) {
            val analyzer = builtin.computeIfAbsent(player) { CombinedAnalyzer() }
            getInferKeyBindingCombined(action).filterIsInstance<Combined>().forEach { binding ->
                // 注册到分析器内
                if (CombinedEvent.Begin(player, binding).call()) {
                    analyzer.registerCombined(binding)
                }
            }
            // 压入行为按键
            analyzer.process(action).forEach { inference ->
                CombinedEvent.Close(player, inference).call()
            }
            analyzer.inferences.forEach {
                CombinedEvent.PressIn(player,it,action).call()
            }
        }

        fun getInferKeyBindingCombined(action: InteractionAction): List<KeyBinding> {
            return KeyBindingAPI.getValues().filter {
                (it as Combined).mapping[0] == action.code
            }
        }

        fun createInference(combined: Combined): Inference {
            return if (combined.matchingType == Combined.MatchingType.STRICT) {
                Strict(combined)
            } else {
                Fuzzy(combined)
            }
        }

    }

    interface Inference {

        val combined: Combined

        fun isClosedWithRequestTick(): Boolean

        fun pressIn(action: InteractionAction): Boolean

    }

    class Fuzzy(combined: Combined) : AbstractInference(combined) {

        val include = mutableListOf<InteractionAction>()

        override fun pressIn(action: InteractionAction): Boolean {
            // 模糊匹配 如果没有录入 则录入进去
            if (!include.contains(action)) {
                include += action
            }

            return include.size == combined.mapping.size
        }

    }

    class Strict(combined: Combined) : AbstractInference(combined) {

        /** 0已经匹配过了 从1开始 */
        var pointer = 0

        val samples = combined.mapping

        override fun pressIn(action: InteractionAction): Boolean {
            if (samples[pointer] == action.code) {
                pointer++
            }
            // 如果是最后一个元素 直接返回 true
            if (pointer == samples.size) {
                return true
            }

            return false
        }

    }

    abstract class AbstractInference(override val combined: Combined) : Inference {

        val createTime = System.currentTimeMillis()


        override fun isClosedWithRequestTick(): Boolean {
            return System.currentTimeMillis() - createTime > combined.requestTick * 50
        }
    }

}
