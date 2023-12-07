package com.gitee.planners.api.script

import java.util.concurrent.CompletableFuture

interface KetherScript : Script {

    fun run(options: KetherScriptOptions): CompletableFuture<Any?>

}
