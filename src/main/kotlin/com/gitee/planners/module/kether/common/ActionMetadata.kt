package com.gitee.planners.module.kether.common

import com.gitee.planners.api.common.metadata.metadata
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.OperationKetherParser
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainerization
import com.gitee.planners.module.kether.catchParsedAction
import com.gitee.planners.module.kether.expectTargetContainerParsedAction
import com.gitee.planners.module.kether.runTargetContainer
import taboolib.module.kether.*

@CombinationKetherParser.Used
object ActionMetadata : OperationKetherParser("metadata") {

    @Suppress("NAME_SHADOWING")
    @KetherEditor.Document(value = "metadata <id> [def <value>] [at objective:TargetContainer(sender)]", result = Void::class)
    val main = argumentKetherParser("get") { argument ->
        // 使用 字符串代替 null
        val defaultValue = this.catchParsedAction("def", "__NULL__")
        val container = this.expectTargetContainerParsedAction(LeastType.SENDER)
        actionFuture { f ->
            this.run(argument).str { id ->
                this.run(defaultValue).thenAccept { defaultValue ->
                    this.runTargetContainer(container).thenAccept {
                        val defaultValue = if (defaultValue == "__NULL__") null else defaultValue
                        f.complete(it.filterIsInstance<TargetContainerization>().first().getMetadata(id)?.any() ?: defaultValue)
                    }.except { null }
                }
            }
        }
    }

    @KetherEditor.Document("metadata <id> to <value:Any> [timeout: long(-1)] [at objective:TargetContainer(sender)]")
    val to = argumentKetherParser { argument ->
        val data = this.nextParsedAction()
        val timeout = this.catchParsedAction("timeout", -1)
        val container = this.expectTargetContainerParsedAction(LeastType.SENDER)
        actionNow {
            this.run(argument).str { id ->
                this.run(data).thenAccept { data ->
                    this.run(timeout).long { timeout ->
                        val metadata = data.metadata(timeout * 50)
                        this.runTargetContainer(container).thenAccept {
                            it.filterIsInstance<TargetContainerization>().forEach { entity ->
                                entity.setMetadata(id, metadata)
                            }
                        }
                    }
                }
            }
        }
    }

    @KetherEditor.Document("metadata <id> add <value:Number> [at objective:TargetContainer(sender)]")
    val add = argumentKetherParser { argument ->
        val data = this.nextParsedAction()
        val container = this.expectTargetContainerParsedAction(LeastType.SENDER)
        actionNow {
            this.run(argument).str { id ->
                this.run(data).thenAccept { data ->
                    this.runTargetContainer(container).thenAccept {
                        it.filterIsInstance<TargetContainerization>().forEach { entity ->
                            entity.getMetadata(id)?.increase(data ?: 0)
                        }
                    }
                }
            }
        }
    }

}
