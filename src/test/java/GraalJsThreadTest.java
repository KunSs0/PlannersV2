import com.gitee.planners.module.script.ScriptContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 直接测试 GraalJS + ScriptContext 的线程行为
 */
public class GraalJsThreadTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== GraalJS + ScriptContext 线程测试 ===\n");

        // 测试1: 同步执行
        testSync();

        // 测试2: 异步执行，ScriptContext 在异步线程中设置
        testAsyncInSameThread();

        // 测试3: 异步执行，ScriptContext 在主线程设置
        testAsyncMainThreadSet();
    }

    static void testSync() throws Exception {
        System.out.println("[测试1] 同步执行:");
        Map<String, Object> vars = createVars("SyncPlayer");
        ScriptContext.setCurrent(vars);

        String script =
            "var sender = senderFunc();\n" +
            "if (sender === null) { 'NULL' } else { sender };";

        try (Context ctx = createContext()) {
            ctx.getBindings("js").putMember("senderFunc", (java.util.function.Supplier<Object>) ScriptContext::getSender);
            Value result = ctx.eval("js", script);
            String value = result.asString();
            System.out.println("  结果: " + value);
            System.out.println("  状态: " + ("SyncPlayer".equals(value) ? "✓ 通过" : "✗ 失败"));
        }

        ScriptContext.clear();
        System.out.println();
    }

    static void testAsyncInSameThread() throws Exception {
        System.out.println("[测试2] 异步执行 (在异步线程中设置 ScriptContext):");
        Map<String, Object> vars = createVars("AsyncPlayer1");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("  线程: " + Thread.currentThread().getName());
            ScriptContext.setCurrent(vars);

            String script =
                "var sender = senderFunc();\n" +
                "if (sender === null) { 'NULL' } else { sender };";

            try (Context ctx = createContext()) {
                ctx.getBindings("js").putMember("senderFunc", (java.util.function.Supplier<Object>) ScriptContext::getSender);
                Value result = ctx.eval("js", script);
                return result.asString();
            } finally {
                ScriptContext.clear();
            }
        });

        String value = future.get();
        System.out.println("  结果: " + value);
        System.out.println("  状态: " + ("AsyncPlayer1".equals(value) ? "✓ 通过" : "✗ 失败"));
        System.out.println();
    }

    static void testAsyncMainThreadSet() throws Exception {
        System.out.println("[测试3] 异步执行 (在主线程设置 ScriptContext):");
        Map<String, Object> vars = createVars("AsyncPlayer2");
        ScriptContext.setCurrent(vars);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("  线程: " + Thread.currentThread().getName());

            String script =
                "var sender = senderFunc();\n" +
                "if (sender === null) { 'NULL' } else { sender };";

            try (Context ctx = createContext()) {
                ctx.getBindings("js").putMember("senderFunc", (java.util.function.Supplier<Object>) ScriptContext::getSender);
                Value result = ctx.eval("js", script);
                return result.asString();
            }
        });

        String value = future.get();
        System.out.println("  结果: " + value);
        System.out.println("  预期: null (ThreadLocal 不跨线程)");
        System.out.println("  状态: " + ("NULL".equals(value) ? "✓ 符合预期" : "✗ 不符合预期"));

        ScriptContext.clear();
        System.out.println();
    }

    static Context createContext() {
        return Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup(s -> true)
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    }

    static Map<String, Object> createVars(String sender) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sender", sender);
        return vars;
    }
}
