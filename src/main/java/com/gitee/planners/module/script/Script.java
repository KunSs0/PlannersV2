package com.gitee.planners.module.script;

import java.util.concurrent.CompletableFuture;

/**
 * 脚本接口 (替代 FluxonScript)
 */
public interface Script {

    CompletableFuture<Object> run(ScriptOptions options);
}
