package com.gitee.planners.module.script

import com.novalang.compiler.ast.decl.FunDecl
import com.novalang.compiler.lexer.Lexer
import com.novalang.compiler.parser.Parser
import com.novalang.runtime.CompiledNova
import com.novalang.runtime.Nova
import com.novalang.runtime.NovaSecurityPolicy
import com.novalang.runtime.SchedulerHolder
import com.novalang.runtime.compilation.BytecodeArtifactCache
import com.novalang.runtime.compilation.CompilationConfigLoader
import com.novalang.runtime.compilation.NovaCompiler
import com.novalang.runtime.compilation.SourceUnit
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * Planners 单次加载的独立编译程序，同时拥有所有技能回调和调度资源。
 *
 * @param program 当前加载代际独占的 Nova 程序。
 */
class PlannersScriptProgram private constructor(private val program: CompiledNova) : AutoCloseable {
    private val callbacks = program.callbackContext

    /**
     * 初始化公共模块；技能动作只声明 execute，加载时不会施法。
     *
     * @throws RuntimeException 公共模块初始化失败时抛出。
     */
    fun initialize() {
        try {
            program.run()
        } catch (failure: Throwable) {
            try {
                program.close()
            } catch (cleanup: Throwable) {
                failure.addSuppressed(cleanup)
            }
            throw failure
        }
    }

    /**
     * 以本次独立绑定调用指定模块，纯函数与回调共用程序的串行上下文。
     *
     * @param unit 预编译模块与函数标识。
     * @param bindings 本次隔离的业务绑定。
     * @param args 显式函数参数。
     * @return 脚本返回值。
     * @throws RuntimeException 程序已关闭或脚本调用失败时抛出。
     */
    fun invoke(unit: NovaScriptUnit, bindings: Map<String, Any?>, vararg args: Any?): Any? {
        return program.callModuleIsolated(unit.moduleId, unit.functionName, bindings, callbacks, *args)
    }

    /**
     * 在宿主主线程调用业务函数，保持旧有 Bukkit 线程约束。
     *
     * @param unit 预编译模块与函数标识。
     * @param bindings 本次业务绑定。
     * @param args 显式函数参数。
     * @return 脚本返回值。
     * @throws IllegalStateException 宿主尚未安装调度器时抛出。
     */
    fun invokeBusiness(unit: NovaScriptUnit, bindings: Map<String, Any?>, vararg args: Any?): Any? {
        val scheduler = SchedulerHolder.get()
        check(scheduler != null) { "Planners requires the Nova host scheduler" }
        if (scheduler.isMainThread) {
            return invoke(unit, bindings, *args)
        }
        val executor = scheduler.mainExecutor()
        check(executor != null) { "Planners requires the Nova main thread executor" }
        return CompletableFuture.supplyAsync({ invoke(unit, bindings, *args) }, executor).join()
    }

    /**
     * 注销当前代际的全部资源并使旧回调失效。
     */
    override fun close() {
        program.close()
    }

    companion object {
        /**
         * 从显式启动入口与全部 YAML 虚拟模块编译一个程序，不执行技能。
         *
         * @param configFile 编译别名与诊断配置。
         * @param sources 具有原始 YAML 来源映射的虚拟模块。
         * @param configure 安装宿主类加载器与类型能力。
         * @return 尚未初始化的独立程序。
         * @throws IllegalArgumentException 动作仍声明保留入口 main 时抛出。
         */
        fun compile(configFile: Path, sources: Collection<SourceUnit>, configure: (Nova) -> Unit): PlannersScriptProgram {
            val imports = StringBuilder("import \"@planners/bootstrap\"\n")
            for (source in sources) {
                imports.append("import \"").append(source.moduleId).append("\"\n")
            }
            val nova = Nova(NovaSecurityPolicy.unrestricted())
            configure(nova)
            val metadata = CompilationConfigLoader().load(configFile)
            val entry = SourceUnit("planners.program", imports.toString(), configFile, null, 1, 0, null)
            val program = NovaCompiler().compile(nova, entry, metadata, sources, BytecodeArtifactCache())
            try {
                // 编译器先完成语法检查和来源映射，再拒绝保留入口。
                for (source in sources) {
                    val parsed = Parser(Lexer(source.sourceText, source.moduleId), source.moduleId).parse()
                    for (declaration in parsed.declarations) {
                        require(declaration !is FunDecl || declaration.name != "main") {
                            "Planners action must use execute instead of reserved main: ${source.describeOrigin(declaration.location.line)}"
                        }
                    }
                }
                return PlannersScriptProgram(program)
            } catch (failure: Throwable) {
                try {
                    program.close()
                } catch (cleanup: Throwable) {
                    failure.addSuppressed(cleanup)
                }
                throw failure
            }
        }
    }
}
